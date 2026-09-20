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
}
