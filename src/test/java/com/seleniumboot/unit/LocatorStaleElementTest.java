package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * {@link Locator} terminal actions must survive the page re-rendering the element between
 * resolving it and acting on it — they re-resolve instead of reusing a stale reference.
 */
public class LocatorStaleElementTest {

    private WebDriver driver;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(driver);

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

    private static WebElement liveElement() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        return el;
    }

    @Test
    public void click_elementGoesStaleBetweenCheckAndClick_reResolvesAndClicksFreshElement() {
        WebElement stale = liveElement();
        doThrow(new StaleElementReferenceException("re-rendered")).when(stale).click();
        WebElement fresh = liveElement();
        By by = By.id("save");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        Locator.of(by).click();

        verify(fresh, times(1)).click();
    }

    @Test
    public void click_elementIsStaleWhileWaitingForClickable_reResolves() {
        WebElement stale = mock(WebElement.class);
        when(stale.isDisplayed()).thenThrow(new StaleElementReferenceException("detached"));
        WebElement fresh = liveElement();
        By by = By.id("save");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        Locator.of(by).click();

        verify(fresh, times(1)).click();
    }

    @Test
    public void click_elementNeverGoesStale_clicksExactlyOnce() {
        WebElement el = liveElement();
        By by = By.id("save");
        when(driver.findElements(by)).thenReturn(List.of(el));

        Locator.of(by).click();

        verify(el, times(1)).click();
        assertEquals(mockingDetails(driver).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("findElements")).count(), 1L);
    }

    // ------------------------------------------------------------------
    // Presence wait + the remaining terminal actions
    // ------------------------------------------------------------------

    @Test
    public void click_elementNotInDomYet_waitsForItToAppear() {
        WebElement fresh = liveElement();
        By by = By.id("late");
        when(driver.findElements(by)).thenReturn(List.of()).thenReturn(List.of()).thenReturn(List.of(fresh));

        Locator.of(by).click();

        verify(fresh, times(1)).click();
    }

    @Test
    public void click_elementNeverAppears_throwsLocatorExceptionCausedByTimeout() {
        By by = By.id("never");
        when(driver.findElements(by)).thenReturn(List.of());

        try {
            Locator.of(by).click();
            org.testng.Assert.fail("expected LocatorException");
        } catch (com.seleniumboot.locator.LocatorException e) {
            org.testng.Assert.assertTrue(e.getMessage().contains("No element found"), e.getMessage());
            org.testng.Assert.assertTrue(e.getMessage().contains("waited 2s"), e.getMessage());
            org.testng.Assert.assertTrue(e.getCause() instanceof org.openqa.selenium.TimeoutException);
        }
    }

    @Test
    public void getText_elementGoesStale_reResolves() {
        WebElement stale = mock(WebElement.class);
        when(stale.isDisplayed()).thenReturn(true);
        when(stale.getText()).thenThrow(new StaleElementReferenceException("re-rendered"));
        WebElement fresh = liveElement();
        when(fresh.getText()).thenReturn("  hello ");
        By by = By.id("label");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        assertEquals(Locator.of(by).getText(), "hello");
    }

    @Test
    public void getAttribute_nullValueIsReturnedNotRetried() {
        WebElement el = liveElement();
        when(el.getAttribute("data-x")).thenReturn(null);
        By by = By.id("attr");
        when(driver.findElements(by)).thenReturn(List.of(el));

        org.testng.Assert.assertNull(Locator.of(by).getAttribute("data-x"));
        verify(el, times(1)).getAttribute("data-x");
    }

    @Test
    public void type_elementGoesStale_reResolvesAndTypesIntoFreshElement() {
        WebElement stale = liveElement();
        doThrow(new StaleElementReferenceException("re-rendered")).when(stale).clear();
        WebElement fresh = liveElement();
        By by = By.id("name");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        Locator.of(by).type("abc");

        verify(fresh).clear();
        verify(fresh).sendKeys("abc");
    }

    @Test
    public void append_elementGoesStale_reResolves() {
        WebElement stale = liveElement();
        doThrow(new StaleElementReferenceException("re-rendered")).when(stale).sendKeys("x");
        WebElement fresh = liveElement();
        By by = By.id("name");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        Locator.of(by).append("x");

        verify(fresh).sendKeys("x");
    }

    @Test
    public void jsClick_elementGoesStale_reResolves() {
        WebDriver jsDriver = mock(WebDriver.class, withSettings().extraInterfaces(org.openqa.selenium.JavascriptExecutor.class));
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) jsDriver;
        driverManagerMock.when(DriverManager::getDriver).thenReturn(jsDriver);
        WebElement stale = liveElement();
        WebElement fresh = liveElement();
        By by = By.id("obscured");
        when(jsDriver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));
        when(js.executeScript(anyString(), eq(stale))).thenThrow(new StaleElementReferenceException("re-rendered"));

        Locator.of(by).jsClick();

        verify(js).executeScript(anyString(), eq(fresh));
    }
}
