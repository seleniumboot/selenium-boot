package com.seleniumboot.locator;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link LocatorResolution}: container scoping, accessible-name, CSS and text
 * filters, root selectors per kind, and the XPath/CSS escaping helpers. Mocked driver, no browser.
 */
public class LocatorResolutionTest {

    private WebDriver driver;
    private JavascriptExecutor js;

    @BeforeMethod
    public void setUp() {
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        js = (JavascriptExecutor) driver;
        DriverManager.pushSessionOverride(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.popSessionOverride();
    }

    // ------------------------------------------------------------------
    // within(container)
    // ------------------------------------------------------------------

    @Test
    public void within_searchesInsideTheContainer() {
        WebElement container = mock(WebElement.class);
        WebElement inside = mock(WebElement.class);
        when(driver.findElement(By.id("form"))).thenReturn(container);
        when(container.findElements(By.cssSelector("input"))).thenReturn(List.of(inside));

        List<WebElement> found = LocatorResolution.resolveAll(Locator.ofCss("input").within(By.id("form")));

        assertEquals(found, List.of(inside));
        verify(driver, never()).findElements(any(By.class));
    }

    @Test
    public void within_missingContainer_throwsLocatorExceptionNamingIt() {
        when(driver.findElement(By.id("gone"))).thenThrow(new NoSuchElementException("no such element"));

        LocatorException e = expectThrows(LocatorException.class,
                () -> LocatorResolution.resolveAll(Locator.ofCss("input").within(By.id("gone"))));

        assertTrue(e.getMessage().startsWith("Container not found: By.id: gone"), e.getMessage());
    }

    // ------------------------------------------------------------------
    // withName(...) — accessible-name filter
    // ------------------------------------------------------------------

    @Test
    public void withName_keepsCaseInsensitiveSubstringMatchesByDefault() {
        WebElement save = element();
        WebElement cancel = element();
        WebElement unnamed = element();
        WebElement broken = element();
        when(driver.findElements(any(By.class))).thenReturn(List.of(save, cancel, unnamed, broken));
        name(save, "  Save Changes ");
        name(cancel, "Cancel");
        name(unnamed, null);
        when(js.executeScript(eq(LocatorJs.ACCESSIBLE_NAME_JS), eq(broken)))
                .thenThrow(new WebDriverException("stale"));

        List<WebElement> found = LocatorResolution.resolveAll(Locator.byRole(Role.BUTTON).withName("save"));

        assertEquals(found, List.of(save));
    }

    @Test
    public void withName_exact_requiresTheWholeTrimmedName() {
        WebElement save = element();
        WebElement saveAll = element();
        when(driver.findElements(any(By.class))).thenReturn(List.of(save, saveAll));
        name(save, " Save ");
        name(saveAll, "Save all");

        List<WebElement> found = LocatorResolution.resolveAll(
                Locator.byRole(Role.BUTTON).withName("Save").exact());

        assertEquals(found, List.of(save));
    }

    // ------------------------------------------------------------------
    // filter(css)
    // ------------------------------------------------------------------

    @Test
    public void filter_keepsOnlyElementsMatchingTheSelector() {
        WebElement active = element();
        WebElement inactive = element();
        WebElement erroring = element();
        when(driver.findElements(By.cssSelector("li"))).thenReturn(List.of(active, inactive, erroring));
        matches(active, ".active", true);
        matches(inactive, ".active", false);
        when(js.executeScript(anyString(), eq(erroring), eq(".active"))).thenThrow(new WebDriverException("detached"));

        List<WebElement> found = LocatorResolution.resolveAll(Locator.ofCss("li").filter(".active"));

        assertEquals(found, List.of(active));
    }

    @Test
    public void matchesCss_isFalseWhenTheScriptFails() {
        WebElement el = element();
        when(js.executeScript(anyString(), eq(el), eq(".x"))).thenThrow(new WebDriverException("boom"));

        assertFalse(LocatorResolution.matchesCss(Locator.ofCss("div"), el, ".x"));
    }

    @Test
    public void matchesCss_isTrueOnlyForBooleanTrue() {
        WebElement yes = element();
        WebElement odd = element();
        matches(yes, ".x", true);
        when(js.executeScript(anyString(), eq(odd), eq(".x"))).thenReturn("true");

        assertTrue(LocatorResolution.matchesCss(Locator.ofCss("div"), yes, ".x"));
        assertFalse(LocatorResolution.matchesCss(Locator.ofCss("div"), odd, ".x"));
    }

    // ------------------------------------------------------------------
    // withText(...)
    // ------------------------------------------------------------------

    @Test
    public void withText_matchesTrimmedTextExactly_andSkipsElementsThatThrow() {
        WebElement match = element();
        WebElement partial = element();
        WebElement stale = element();
        when(match.getText()).thenReturn("  Delete  ");
        when(partial.getText()).thenReturn("Delete all");
        when(stale.getText()).thenThrow(new WebDriverException("stale element"));
        when(driver.findElements(By.cssSelector("button"))).thenReturn(List.of(match, partial, stale));

        List<WebElement> found = LocatorResolution.resolveAll(Locator.ofCss("button").withText("Delete"));

        assertEquals(found, List.of(match));
    }

    // ------------------------------------------------------------------
    // nth / first error messages
    // ------------------------------------------------------------------

    @Test
    public void first_onNoMatch_namesFirstInTheMessage() {
        when(driver.findElements(any(By.class))).thenReturn(List.of());

        LocatorException e = expectThrows(LocatorException.class,
                () -> LocatorResolution.resolveAll(Locator.ofCss("tr").first()));

        assertTrue(e.getMessage().startsWith("first() requested but only 0 element(s) matched"), e.getMessage());
    }

    @Test
    public void nth_outOfRange_namesTheIndexInTheMessage() {
        when(driver.findElements(any(By.class))).thenReturn(List.of(element()));

        LocatorException e = expectThrows(LocatorException.class,
                () -> LocatorResolution.resolveAll(Locator.ofCss("tr").nth(3)));

        assertTrue(e.getMessage().startsWith("nth(3) requested but only 1 element(s) matched"), e.getMessage());
    }

    // ------------------------------------------------------------------
    // buildRoot per kind
    // ------------------------------------------------------------------

    @Test
    public void buildRoot_headingWithLevel_targetsTagAndAriaLevel() {
        By root = LocatorResolution.buildRoot(Locator.byRole(Role.HEADING).withLevel(2));

        assertEquals(root, By.cssSelector("h2, [role='heading'][aria-level='2']"));
    }

    @Test
    public void buildRoot_attributeKinds_useContainsByDefaultAndEqualityWhenExact() {
        assertEquals(LocatorResolution.buildRoot(Locator.byPlaceholder("Email")),
                By.cssSelector("[placeholder*='Email' i]"));
        assertEquals(LocatorResolution.buildRoot(Locator.byAltText("Logo").exact()),
                By.cssSelector("[alt='Logo']"));
        assertEquals(LocatorResolution.buildRoot(Locator.byTitle("Close")),
                By.cssSelector("[title*='Close' i]"));
    }

    @Test
    public void buildRoot_labelKind_targetsFormControls() {
        assertEquals(LocatorResolution.buildRoot(Locator.byLabel("Name")), By.cssSelector(LocatorJs.FORM_CONTROL_CSS));
    }

    // ------------------------------------------------------------------
    // Escaping helpers
    // ------------------------------------------------------------------

    @Test
    public void xpathLiteral_quotesSafelyForEveryQuoteMix() {
        assertEquals(LocatorResolution.xpathLiteral("plain"), "'plain'");
        assertEquals(LocatorResolution.xpathLiteral("it's"), "\"it's\"");
        assertEquals(LocatorResolution.xpathLiteral("say \"it's\" now"),
                "concat('say \"it', \"'\", 's\" now')");
        assertEquals(LocatorResolution.xpathLiteral("'edge'\""),
                "concat('', \"'\", 'edge', \"'\", '\"')");
    }

    @Test
    public void cssEscape_escapesBackslashesAndSingleQuotes() {
        assertEquals(LocatorResolution.cssEscape("a\\b'c"), "a\\\\b\\'c");
    }

    // ------------------------------------------------------------------

    private static WebElement element() {
        return mock(WebElement.class);
    }

    private void name(WebElement el, String accessibleName) {
        when(js.executeScript(eq(LocatorJs.ACCESSIBLE_NAME_JS), eq(el))).thenReturn(accessibleName);
    }

    private void matches(WebElement el, String css, boolean result) {
        when(js.executeScript(anyString(), eq(el), eq(css))).thenReturn(result);
    }
}
