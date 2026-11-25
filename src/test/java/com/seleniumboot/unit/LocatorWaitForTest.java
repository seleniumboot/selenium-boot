package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import com.seleniumboot.locator.State;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorWaitForTest {


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

    // ------------------------------------------------------------------
    // waitFor(State), allTextContents, and screenshot
    // ------------------------------------------------------------------

    @Test
    public void waitFor_visible_succeedsWhenDisplayed() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(mockDriver.findElements(By.id("toast"))).thenReturn(List.of(el));

        Locator.of(By.id("toast")).waitFor(State.VISIBLE);
        // Completed without exception
    }

    @Test(expectedExceptions = LocatorException.class)
    public void waitFor_visible_throwsWhenElementNeverAppears() {
        when(mockDriver.findElements(By.id("missing"))).thenReturn(Collections.emptyList());

        Locator.of(By.id("missing")).waitFor(State.VISIBLE);
    }

    @Test(expectedExceptions = LocatorException.class)
    public void waitFor_visible_throwsWhenElementNotDisplayed() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(false);
        when(mockDriver.findElements(By.id("hidden-box"))).thenReturn(List.of(el));

        Locator.of(By.id("hidden-box")).waitFor(State.VISIBLE);
    }

    @Test
    public void waitFor_default_waitsForVisible() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(mockDriver.findElements(By.id("toast"))).thenReturn(List.of(el));

        Locator.of(By.id("toast")).waitFor();
    }

    @Test
    public void waitFor_hidden_succeedsWhenNotDisplayed() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(false);
        when(mockDriver.findElements(By.id("spinner"))).thenReturn(List.of(el));

        Locator.of(By.id("spinner")).waitFor(State.HIDDEN);
    }

    @Test
    public void waitFor_hidden_succeedsWhenNotInDom() {
        when(mockDriver.findElements(By.id("spinner"))).thenReturn(Collections.emptyList());

        Locator.of(By.id("spinner")).waitFor(State.HIDDEN);
    }

    @Test(expectedExceptions = LocatorException.class)
    public void waitFor_hidden_throwsWhenElementRemainsVisible() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(mockDriver.findElements(By.id("banner"))).thenReturn(List.of(el));

        Locator.of(By.id("banner")).waitFor(State.HIDDEN);
    }

    @Test
    public void waitFor_attached_succeedsWhenInDomEvenIfHidden() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(false);
        when(el.isEnabled()).thenReturn(true);
        when(mockDriver.findElements(By.id("hidden-input"))).thenReturn(List.of(el));

        Locator.of(By.id("hidden-input")).waitFor(State.ATTACHED);
    }

    @Test(expectedExceptions = LocatorException.class)
    public void waitFor_attached_throwsWhenElementNotInDom() {
        when(mockDriver.findElements(By.id("missing"))).thenReturn(Collections.emptyList());

        Locator.of(By.id("missing")).waitFor(State.ATTACHED);
    }

    @Test
    public void waitFor_detached_succeedsWhenNotInDom() {
        when(mockDriver.findElements(By.id("removed"))).thenReturn(Collections.emptyList());

        Locator.of(By.id("removed")).waitFor(State.DETACHED);
    }

    @Test(expectedExceptions = LocatorException.class)
    public void waitFor_detached_throwsWhenElementRemainsInDom() {
        WebElement el = mock(WebElement.class);
        when(el.isEnabled()).thenReturn(true);
        when(mockDriver.findElements(By.id("present"))).thenReturn(List.of(el));

        Locator.of(By.id("present")).waitFor(State.DETACHED);
    }

}
