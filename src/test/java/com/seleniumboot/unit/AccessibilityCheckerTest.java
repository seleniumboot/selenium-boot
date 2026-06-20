package com.seleniumboot.unit;

import com.seleniumboot.accessibility.AccessibilityAssert;
import com.seleniumboot.accessibility.AccessibilityChecker;
import com.seleniumboot.accessibility.AccessibilityResult;
import com.seleniumboot.accessibility.AccessibilityViolation;
import com.seleniumboot.accessibility.Impact;
import com.seleniumboot.driver.DriverManager;
import org.mockito.MockedStatic;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link AccessibilityChecker} and {@link AccessibilityAssert}.
 *
 * Uses a mocked {@link JavascriptExecutor} WebDriver to simulate axe-core responses
 * without a real browser.
 */
public class AccessibilityCheckerTest {

    private WebDriver mockDriver;
    private MockedStatic<DriverManager> driverManagerMock;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(mockDriver);

        // axe already loaded — skip injection by default
        when(((JavascriptExecutor) mockDriver).executeScript(
                contains("typeof axe")))
                .thenReturn(Boolean.TRUE);
    }

    @AfterMethod
    public void teardown() {
        driverManagerMock.close();
    }

    // ── Impact enum tests ─────────────────────────────────────────────────────

    @Test
    public void impact_fromString_parsesAllLevels() {
        assertEquals(Impact.fromString("critical"), Impact.CRITICAL);
        assertEquals(Impact.fromString("serious"),  Impact.SERIOUS);
        assertEquals(Impact.fromString("moderate"), Impact.MODERATE);
        assertEquals(Impact.fromString("minor"),    Impact.MINOR);
    }

    @Test
    public void impact_fromString_unknownReturnsMinor() {
        assertEquals(Impact.fromString("unknown"), Impact.MINOR);
        assertEquals(Impact.fromString(null),      Impact.MINOR);
    }

    @Test
    public void impact_isAtLeast_ordering() {
        assertTrue(Impact.CRITICAL.isAtLeast(Impact.CRITICAL));
        assertTrue(Impact.CRITICAL.isAtLeast(Impact.SERIOUS));
        assertTrue(Impact.CRITICAL.isAtLeast(Impact.MINOR));
        assertTrue(Impact.SERIOUS.isAtLeast(Impact.MODERATE));

        assertFalse(Impact.MINOR.isAtLeast(Impact.MODERATE));
        assertFalse(Impact.MODERATE.isAtLeast(Impact.SERIOUS));
        assertFalse(Impact.SERIOUS.isAtLeast(Impact.CRITICAL));
    }

    // ── AccessibilityResult tests ─────────────────────────────────────────────

    @Test
    public void result_isClean_whenNoViolations() {
        AccessibilityResult result = new AccessibilityResult("http://example.com",
                List.of(), 5, 1);
        assertTrue(result.isClean());
        assertEquals(result.violationCount(), 0);
        assertEquals(result.passCount(), 5);
    }

    @Test
    public void result_violationsAtLevel_filtersCorrectly() {
        AccessibilityViolation minor    = violation("rule-1", Impact.MINOR);
        AccessibilityViolation moderate = violation("rule-2", Impact.MODERATE);
        AccessibilityViolation critical = violation("rule-3", Impact.CRITICAL);

        AccessibilityResult result = new AccessibilityResult(
                "http://example.com", List.of(minor, moderate, critical), 0, 0);

        assertEquals(result.violationsAtLevel(Impact.CRITICAL).size(), 1);
        assertEquals(result.violationsAtLevel(Impact.MODERATE).size(), 2);
        assertEquals(result.violationsAtLevel(Impact.MINOR).size(), 3);
    }

    // ── AccessibilityChecker.scan() tests ─────────────────────────────────────

    @Test
    public void scan_cleanPage_returnsNoViolations() {
        stubAxeResponse(axeResponse(List.of(), 10, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com/home");

        AccessibilityResult result = AccessibilityChecker.scan();

        assertTrue(result.isClean());
        assertEquals(result.url(), "http://example.com/home");
        assertEquals(result.passCount(), 10);
    }

    @Test
    public void scan_withViolations_parsesImpactAndNodes() {
        Map<String, Object> node = Map.of(
                "html", "<img src='foo.png'>",
                "target", List.of("img.hero"),
                "failureSummary", "Fix any of the following: Element does not have an alt attribute"
        );
        Map<String, Object> violation = Map.of(
                "id", "image-alt",
                "description", "Ensures <img> elements have alternate text",
                "help", "Images must have alternate text",
                "helpUrl", "https://dequeuniversity.com/rules/axe/4.10/image-alt",
                "impact", "critical",
                "nodes", List.of(node)
        );
        stubAxeResponse(axeResponse(List.of(violation), 5, 2));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com");

        AccessibilityResult result = AccessibilityChecker.scan();

        assertFalse(result.isClean());
        assertEquals(result.violationCount(), 1);

        AccessibilityViolation v = result.violations().get(0);
        assertEquals(v.id(), "image-alt");
        assertEquals(v.impact(), Impact.CRITICAL);
        assertEquals(v.nodes().size(), 1);
        assertEquals(v.nodes().get(0).target(), "img.hero");
    }

    @Test
    public void scan_noDriver_throwsIllegalState() {
        driverManagerMock.when(DriverManager::getDriver).thenReturn(null);

        boolean threw = false;
        try {
            AccessibilityChecker.scan();
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "IllegalStateException expected when no driver is active");
    }

    // ── AccessibilityAssert tests ─────────────────────────────────────────────

    @Test
    public void assert_run_passesWhenNoViolations() {
        stubAxeResponse(axeResponse(List.of(), 8, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com");

        // Must not throw
        AccessibilityAssert.create().run();
    }

    @Test
    public void assert_run_throwsWhenViolationsFound() {
        Map<String, Object> violation = Map.of(
                "id", "color-contrast",
                "description", "Ensures the contrast ratio between foreground and background colors",
                "help", "Elements must have sufficient color contrast",
                "helpUrl", "https://dequeuniversity.com/rules/axe/4.10/color-contrast",
                "impact", "serious",
                "nodes", List.of(Map.of(
                        "html", "<p class='light'>text</p>",
                        "target", List.of("p.light"),
                        "failureSummary", "Fix any of the following: Element has insufficient color contrast"
                ))
        );
        stubAxeResponse(axeResponse(List.of(violation), 3, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com/page");

        AssertionError err = null;
        try {
            AccessibilityAssert.create().run();
        } catch (AssertionError e) {
            err = e;
        }
        assertNotNull(err, "AssertionError expected but not thrown");
        assertTrue(err.getMessage().contains("color-contrast"), "Error must name the rule");
        assertTrue(err.getMessage().contains("SERIOUS"),        "Error must show impact level");
        assertTrue(err.getMessage().contains("p.light"),        "Error must show the offending node");
    }

    @Test
    public void assert_withLevel_skipsViolationsBelowThreshold() {
        Map<String, Object> minorViolation = Map.of(
                "id", "landmark-one-main",
                "description", "Ensures the document has a main landmark",
                "help", "Document should have one main landmark",
                "helpUrl", "https://dequeuniversity.com/rules/axe/4.10/landmark-one-main",
                "impact", "moderate",
                "nodes", List.of(Map.of("html", "<body>", "target", List.of("body"),
                        "failureSummary", "Fix the following: Document does not have a main landmark"))
        );
        stubAxeResponse(axeResponse(List.of(minorViolation), 0, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com");

        // CRITICAL threshold: moderate violation must not fail the test
        AccessibilityAssert.create().withLevel(Impact.CRITICAL).run();
    }

    @Test
    public void assert_collect_returnsResultWithoutThrowing() {
        Map<String, Object> violation = Map.of(
                "id", "button-name",
                "description", "Ensures buttons have discernible text",
                "help", "Buttons must have discernible text",
                "helpUrl", "https://dequeuniversity.com/rules/axe/4.10/button-name",
                "impact", "critical",
                "nodes", List.of(Map.of("html", "<button></button>", "target", List.of("button"),
                        "failureSummary", "Fix any of the following: Element does not have inner text"))
        );
        stubAxeResponse(axeResponse(List.of(violation), 2, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com");

        // collect() must NOT throw even with violations
        AccessibilityResult result = AccessibilityAssert.create().collect();
        assertEquals(result.violationCount(), 1);
    }

    @Test
    public void assert_errorMessage_containsUrlAndCount() {
        Map<String, Object> v1 = minimalViolation("image-alt",    "critical");
        Map<String, Object> v2 = minimalViolation("color-contrast","serious");
        stubAxeResponse(axeResponse(List.of(v1, v2), 0, 0));
        when(mockDriver.getCurrentUrl()).thenReturn("http://example.com/form");

        AssertionError err = null;
        try {
            AccessibilityAssert.create().run();
        } catch (AssertionError e) {
            err = e;
        }
        assertNotNull(err, "AssertionError expected but not thrown");
        assertTrue(err.getMessage().contains("2 violations"), "Count must appear in message");
        assertTrue(err.getMessage().contains("http://example.com/form"), "URL must appear in message");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubAxeResponse(Map<String, Object> response) {
        when(((JavascriptExecutor) mockDriver).executeAsyncScript(anyString()))
                .thenReturn(response);
    }

    private Map<String, Object> axeResponse(List<Map<String, Object>> violations,
                                             int passes, int incomplete) {
        return Map.of(
                "violations", violations,
                "passes",     buildPlaceholderList(passes),
                "incomplete", buildPlaceholderList(incomplete)
        );
    }

    private List<Map<String, Object>> buildPlaceholderList(int size) {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) list.add(Map.of("id", "rule-" + i));
        return list;
    }

    private AccessibilityViolation violation(String id, Impact impact) {
        return new AccessibilityViolation(id, "desc", "help", "http://example.com",
                impact, List.of());
    }

    private Map<String, Object> minimalViolation(String id, String impact) {
        return Map.of(
                "id", id,
                "description", "desc",
                "help", "help text",
                "helpUrl", "https://example.com",
                "impact", impact,
                "nodes", List.of(Map.of("html", "<div>", "target", List.of("div"),
                        "failureSummary", "Fix it"))
        );
    }
}
