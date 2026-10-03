package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** {@code debug.highlight} and {@code debug.slowMoMs}: opt-in, no-op by default. */
public class LocatorDebugTest {

    private interface JsDriver extends WebDriver, JavascriptExecutor {}

    private JsDriver driver;
    private WebElement el;
    private SeleniumBootConfig config;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        driver = mock(JsDriver.class);
        el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(driver.findElements(By.id("go"))).thenReturn(List.of(el));

        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(driver);

        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(2);
        config = new SeleniumBootConfig();
        config.setTimeouts(timeouts);
        contextMock = mockStatic(SeleniumBootContext.class);
        contextMock.when(SeleniumBootContext::getConfig).thenReturn(config);
    }

    @AfterMethod
    public void teardown() {
        driverManagerMock.close();
        contextMock.close();
    }

    @Test
    public void defaults_areOff() {
        assertEquals(config.getDebug().getSlowMoMs(), 0L);
        assertFalse(config.getDebug().isHighlight());
    }

    @Test
    public void setDebugNull_fallsBackToDefaults() {
        config.setDebug(null);
        assertFalse(config.getDebug().isHighlight());
    }

    @Test
    public void disabled_neverTouchesThePageOrSleeps() {
        long start = System.nanoTime();
        Locator.of(By.id("go")).click();
        long ms = (System.nanoTime() - start) / 1_000_000;

        verify(driver, never()).executeScript(anyString(), any(Object[].class));
        assertTrue(ms < 200, "default run must not be slowed, took " + ms + "ms");
    }

    @Test
    public void highlight_outlinesElementBeforeClick() {
        config.getDebug().setHighlight(true);

        Locator.of(By.id("go")).click();

        var order = inOrder(driver, el);
        order.verify((JavascriptExecutor) driver).executeScript(contains("outline"), eq(el));
        order.verify(el).click();
    }

    @Test
    public void highlight_scriptFailureDoesNotFailTheAction() {
        config.getDebug().setHighlight(true);
        when(driver.executeScript(anyString(), any(Object[].class))).thenThrow(new RuntimeException("blocked"));

        Locator.of(By.id("go")).click();

        verify(el).click();
    }

    @Test
    public void slowMo_pausesAfterTheAction() {
        config.getDebug().setSlowMoMs(300);

        long start = System.nanoTime();
        Locator.of(By.id("go")).click();
        long ms = (System.nanoTime() - start) / 1_000_000;

        verify(el).click();
        assertTrue(ms >= 300, "expected >=300ms pause, took " + ms + "ms");
    }

    @Test
    public void negativeSlowMo_clampsToZero() {
        config.getDebug().setSlowMoMs(-5);
        assertEquals(config.getDebug().getSlowMoMs(), 0L);
    }
}
