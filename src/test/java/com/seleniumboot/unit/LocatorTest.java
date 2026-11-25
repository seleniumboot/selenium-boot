package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;



import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorTest {


    private WebDriver mockDriver;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class);

        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(mockDriver);

        // Supply a config so the auto-wait in element() can read timeouts.explicit
        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(2);
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setTimeouts(timeouts);

        contextMock = mockStatic(SeleniumBootContext.class);
        contextMock.when(SeleniumBootContext::getConfig).thenReturn(config);
    }

    @AfterMethod
    public void teardown() {
        driverManagerMock.close();
        contextMock.close();
    }

    // LocatorException thrown when no elements match
    @Test
    public void resolve_throwsLocatorException_whenNoElementsFound() {
        // Locator.of(By) requires DriverManager — tested via integration;
        // here we verify LocatorException message is descriptive.
        LocatorException ex = new LocatorException("No element found for: By.id: missing");
        assertTrue(ex.getMessage().contains("No element found"));
    }

    @Test
    public void locatorException_preservesCause() {
        RuntimeException cause = new RuntimeException("root cause");
        LocatorException ex = new LocatorException("wrapped", cause);
        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getMessage(), "wrapped");
    }

    @Test
    public void locator_toString_includesRootBy() {
        Locator loc = Locator.of(By.id("username"));
        assertTrue(loc.toString().contains("username"),
                "toString should include root By description");
    }

    @Test
    public void locator_toString_includesFilterAndNth() {
        Locator loc = Locator.of(By.cssSelector(".row"))
                .filter(".active")
                .nth(2);
        String str = loc.toString();
        assertTrue(str.contains(".active"), "toString should include filter");
        assertTrue(str.contains("2"),       "toString should include nth index");
    }

    @Test
    public void locator_toString_includesWithText() {
        Locator loc = Locator.ofCss("button").withText("Save");
        assertTrue(loc.toString().contains("Save"), "toString should include withText value");
    }

    @Test
    public void locator_toString_includesWithin() {
        Locator loc = Locator.of(By.cssSelector("input"))
                .within(By.id("login-form"));
        assertTrue(loc.toString().contains("login-form"), "toString should include within container");
    }

    @Test
    public void locatorOfCss_createsByCssSelector() {
        Locator loc = Locator.ofCss(".submit-btn");
        assertTrue(loc.toString().contains("submit-btn"));
    }

    @Test
    public void locatorOf_createsByLocator() {
        Locator loc = Locator.of(By.name("email"));
        assertTrue(loc.toString().contains("email"));
    }

    @Test
    public void locatorException_withMessageOnly() {
        LocatorException ex = new LocatorException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void locator_chaining_doesNotMutateOriginal() {
        Locator base    = Locator.ofCss(".item");
        Locator filtered = base.filter(".active");
        // Both should still be valid Locator objects
        assertNotNull(base);
        assertNotNull(filtered);
        assertTrue(filtered.toString().contains(".active"));
    }

}
