package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.wait.WaitEngine;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for the newer {@link WaitEngine} conditions
 * ({@code waitForAttribute}, {@code waitForUrlMatches}, {@code waitForTextMatches}).
 *
 * <p>Uses a mocked {@link WebDriver} whose state already satisfies each condition,
 * so {@code WebDriverWait} succeeds on the first poll — no real browser required.
 */
public class WaitEngineTest {

    private WebDriver mockDriver;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class);

        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(mockDriver);

        // Supply a config so createWait() can read timeouts.explicit
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

    // ── waitForAttribute (exact match) ─────────────────────────────────────────

    @Test
    public void waitForAttribute_returnsElement_whenAttributeMatchesExactly() {
        By locator = By.id("status");
        WebElement element = mock(WebElement.class);
        when(mockDriver.findElement(locator)).thenReturn(element);
        when(element.getAttribute("aria-expanded")).thenReturn("true");

        WebElement result = WaitEngine.waitForAttribute(locator, "aria-expanded", "true");

        assertSame(result, element);
    }

    // ── waitForUrlMatches (regex) ──────────────────────────────────────────────

    @Test
    public void waitForUrlMatches_returnsTrue_whenUrlMatchesRegex() {
        when(mockDriver.getCurrentUrl()).thenReturn("https://shop.test/orders/42");

        assertTrue(WaitEngine.waitForUrlMatches(".*/orders/\\d+"));
    }

    // ── waitForTextMatches (regex) ─────────────────────────────────────────────

    @Test
    public void waitForTextMatches_returnsElement_whenTextMatchesRegex() {
        By locator = By.cssSelector(".total");
        WebElement element = mock(WebElement.class);
        when(mockDriver.findElement(locator)).thenReturn(element);
        when(element.getText()).thenReturn("$19.99");

        WebElement result = WaitEngine.waitForTextMatches(locator, "\\$\\d+\\.\\d{2}");

        assertSame(result, element);
    }
}
