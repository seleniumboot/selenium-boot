package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorCheckedSelectTest {


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

    // ══════════════════════════════════════════════════════════════════════════
    // Form actions: clear, check, uncheck, isChecked, selectOption, inputValue
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void clear_clearsVisibleElement() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(mockDriver.findElements(By.id("username"))).thenReturn(List.of(el));

        Locator.of(By.id("username")).clear();

        verify(el, times(1)).clear();
    }

    @Test
    public void check_whenUnchecked_clicksElement() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(el.isSelected()).thenReturn(false);
        when(mockDriver.findElements(By.id("agree"))).thenReturn(List.of(el));

        Locator.of(By.id("agree")).check();

        verify(el, times(1)).click();
    }

    @Test
    public void check_whenAlreadyChecked_doesNotClick() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(el.isSelected()).thenReturn(true);
        when(mockDriver.findElements(By.id("agree"))).thenReturn(List.of(el));

        Locator.of(By.id("agree")).check();

        verify(el, never()).click();
    }

    @Test
    public void check_whenAriaCheckedTrue_doesNotClick() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(el.isSelected()).thenReturn(false);
        when(el.getAttribute("aria-checked")).thenReturn("true");
        when(mockDriver.findElements(By.id("custom-chk"))).thenReturn(List.of(el));

        Locator.of(By.id("custom-chk")).check();

        verify(el, never()).click();
    }

    @Test
    public void uncheck_whenChecked_clicksElement() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(el.isSelected()).thenReturn(true);
        when(mockDriver.findElements(By.id("newsletter"))).thenReturn(List.of(el));

        Locator.of(By.id("newsletter")).uncheck();

        verify(el, times(1)).click();
    }

    @Test
    public void uncheck_whenAlreadyUnchecked_doesNotClick() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(true);
        when(el.isSelected()).thenReturn(false);
        when(mockDriver.findElements(By.id("newsletter"))).thenReturn(List.of(el));

        Locator.of(By.id("newsletter")).uncheck();

        verify(el, never()).click();
    }

    @Test
    public void isChecked_returnsTrue_whenSelected() {
        WebElement el = mock(WebElement.class);
        when(el.isSelected()).thenReturn(true);
        when(mockDriver.findElements(By.id("agree"))).thenReturn(List.of(el));

        assertTrue(Locator.of(By.id("agree")).isChecked());
    }

    @Test
    public void isChecked_returnsTrue_whenAriaCheckedTrue() {
        WebElement el = mock(WebElement.class);
        when(el.isSelected()).thenReturn(false);
        when(el.getAttribute("aria-checked")).thenReturn("true");
        when(mockDriver.findElements(By.id("aria-box"))).thenReturn(List.of(el));

        assertTrue(Locator.of(By.id("aria-box")).isChecked());
    }

    @Test
    public void isChecked_returnsFalse_whenUnchecked() {
        WebElement el = mock(WebElement.class);
        when(el.isSelected()).thenReturn(false);
        when(el.getAttribute("aria-checked")).thenReturn("false");
        when(mockDriver.findElements(By.id("agree"))).thenReturn(List.of(el));

        assertFalse(Locator.of(By.id("agree")).isChecked());
    }

    @Test
    public void isChecked_returnsFalse_whenElementNotFound() {
        when(mockDriver.findElements(By.id("missing"))).thenReturn(Collections.emptyList());

        assertFalse(Locator.of(By.id("missing")).isChecked());
    }

    @Test
    public void selectOption_selectsByVisibleText() {
        WebElement selectEl = mock(WebElement.class);
        when(selectEl.getTagName()).thenReturn("select");
        when(selectEl.isDisplayed()).thenReturn(true);
        when(selectEl.isEnabled()).thenReturn(true);
        when(selectEl.getCssValue(anyString())).thenReturn("block");
        WebElement opt = mock(WebElement.class);
        when(opt.isEnabled()).thenReturn(true);
        when(opt.isDisplayed()).thenReturn(true);
        when(opt.getCssValue(anyString())).thenReturn("block");
        when(selectEl.findElements(By.xpath(".//option[normalize-space(.) = \"United Kingdom\"]")))
                .thenReturn(List.of(opt));
        when(mockDriver.findElements(By.id("country"))).thenReturn(List.of(selectEl));

        Locator.of(By.id("country")).selectOption("United Kingdom");

        verify(opt, times(1)).click();
    }

    @Test
    public void selectOptionByValue_selectsByValueAttribute() {
        WebElement selectEl = mock(WebElement.class);
        when(selectEl.getTagName()).thenReturn("select");
        when(selectEl.isDisplayed()).thenReturn(true);
        when(selectEl.isEnabled()).thenReturn(true);
        WebElement opt = mock(WebElement.class);
        when(opt.isEnabled()).thenReturn(true);
        when(selectEl.findElements(By.xpath(".//option[@value = \"gb\"]")))
                .thenReturn(List.of(opt));
        when(mockDriver.findElements(By.id("country"))).thenReturn(List.of(selectEl));

        Locator.of(By.id("country")).selectOptionByValue("gb");

        verify(opt, times(1)).click();
    }

    @Test
    public void inputValue_returnsDomPropertyWhenPresent() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.getDomProperty("value")).thenReturn("admin@test.com");
        when(mockDriver.findElements(By.id("email"))).thenReturn(List.of(el));

        assertEquals(Locator.of(By.id("email")).inputValue(), "admin@test.com");
    }

    @Test
    public void inputValue_fallsBackToAttributeWhenDomPropertyIsNull() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.getDomProperty("value")).thenReturn(null);
        when(el.getAttribute("value")).thenReturn("fallback");
        when(mockDriver.findElements(By.id("email"))).thenReturn(List.of(el));

        assertEquals(Locator.of(By.id("email")).inputValue(), "fallback");
    }

    @Test
    public void inputValue_returnsEmptyStringWhenBothAreNull() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.getDomProperty("value")).thenReturn(null);
        when(el.getAttribute("value")).thenReturn(null);
        when(mockDriver.findElements(By.id("email"))).thenReturn(List.of(el));

        assertEquals(Locator.of(By.id("email")).inputValue(), "");
    }

}
