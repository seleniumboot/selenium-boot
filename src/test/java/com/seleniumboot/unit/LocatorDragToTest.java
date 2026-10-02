package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertThrows;

/** {@link Locator#dragTo}: native gesture first, synthetic HTML5 DnD only when no drop landed. */
public class LocatorDragToTest {

    private WebDriver driver;
    private JavascriptExecutor js;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;
    private WebElement src;
    private WebElement dst;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class, Interactive.class));
        js = (JavascriptExecutor) driver;
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(driver);

        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(1);
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setTimeouts(timeouts);
        contextMock = mockStatic(SeleniumBootContext.class);
        contextMock.when(SeleniumBootContext::getConfig).thenReturn(config);

        src = visible();
        dst = visible();
        when(driver.findElements(By.id("src"))).thenReturn(List.of(src));
        when(driver.findElements(By.id("dst"))).thenReturn(List.of(dst));
    }

    @AfterMethod
    public void teardown() {
        driverManagerMock.close();
        contextMock.close();
    }

    private static WebElement visible() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        return el;
    }

    @Test
    public void dragTo_nativeDropLands_doesNotDispatchSyntheticEvents() {
        when(js.executeScript(contains("__sbDropped === true"), any())).thenReturn(true);

        Locator.of(By.id("src")).dragTo(Locator.of(By.id("dst")));

        verify((Interactive) driver).perform(any());
        verify(js, never()).executeScript(contains("dragstart"), any(), any());
    }

    @Test
    public void dragTo_nativeDropIgnored_fallsBackToSyntheticDataTransfer() {
        when(js.executeScript(contains("__sbDropped === true"), any())).thenReturn(false);

        Locator.of(By.id("src")).dragTo(Locator.of(By.id("dst")));

        verify((Interactive) driver).perform(any());
        verify(js).executeScript(contains("dragstart"), eq(src), eq(dst));
    }

    @Test
    public void dragTo_targetNeverResolves_throwsLocatorException() {
        when(driver.findElements(By.id("missing"))).thenReturn(List.of());

        assertThrows(LocatorException.class,
                () -> Locator.of(By.id("src")).dragTo(Locator.of(By.id("missing"))));
        verify((Interactive) driver, never()).perform(any());
    }

    @Test
    public void dragTo_sourceNeverResolves_throwsLocatorException() {
        when(driver.findElements(By.id("missing"))).thenReturn(List.of());

        assertThrows(LocatorException.class,
                () -> Locator.of(By.id("missing")).dragTo(Locator.of(By.id("dst"))));
        verify((Interactive) driver, never()).perform(any());
    }
}
