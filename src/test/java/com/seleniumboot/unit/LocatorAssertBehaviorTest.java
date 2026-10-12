package com.seleniumboot.unit;

import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;
import static org.testng.Assert.expectThrows;
import static org.testng.Assert.assertTrue;

public class LocatorAssertBehaviorTest {

    private WebDriver driver;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(driver);

        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(1);
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

    @Test
    public void focusedAndEmptyAssertionsUseLiveElementState() {
        By focusedBy = By.id("search");
        WebElement focused = mock(WebElement.class);
        WebDriver.TargetLocator target = mock(WebDriver.TargetLocator.class);
        when(driver.findElements(focusedBy)).thenReturn(List.of(focused));
        when(driver.switchTo()).thenReturn(target);
        when(target.activeElement()).thenReturn(focused);

        SeleniumAssert.assertThat(focusedBy).isFocused();

        By emptyBy = By.id("email");
        WebElement emptyInput = mock(WebElement.class);
        when(driver.findElements(emptyBy)).thenReturn(List.of(emptyInput));
        when(emptyInput.getTagName()).thenReturn("input");
        when(emptyInput.getAttribute("value")).thenReturn("");

        SeleniumAssert.assertThat(emptyBy).isEmpty();
    }

    @Test
    public void regexAssertionsSupportFullAndPartialMatches() {
        By by = By.id("status");
        WebElement element = mock(WebElement.class);
        when(driver.findElements(by)).thenReturn(List.of(element));
        when(element.getText()).thenReturn("Order 412 is ready");
        when(element.getAttribute("value")).thenReturn("user-42");

        SeleniumAssert.assertThat(by).hasText(Pattern.compile("Order \\d+ is ready"));
        SeleniumAssert.assertThat(by).containsText(Pattern.compile("\\d+"));
        SeleniumAssert.assertThat(by).hasValue(Pattern.compile("user-\\d+"));
    }

    @Test
    public void regexAssertionReResolvesAfterStaleElement() {
        By by = By.id("status");
        WebElement stale = mock(WebElement.class);
        WebElement fresh = mock(WebElement.class);
        when(stale.getText()).thenThrow(new StaleElementReferenceException("re-rendered"));
        when(fresh.getText()).thenReturn("ready");
        when(driver.findElements(by)).thenReturn(List.of(stale)).thenReturn(List.of(fresh));

        SeleniumAssert.assertThat(by).hasText(Pattern.compile("ready"));

        verify(driver, atLeast(2)).findElements(by);
    }

    @Test
    public void regexFailureNamesLocatorAndActualValue() {
        By by = By.id("status");
        WebElement element = mock(WebElement.class);
        when(driver.findElements(by)).thenReturn(List.of(element));
        when(element.getText()).thenReturn("pending");

        AssertionError error = expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThat(by).hasText(Pattern.compile("ready")));

        assertTrue(error.getMessage().contains("By.id: status"));
        assertTrue(error.getMessage().contains("actual: [pending]"));
    }
}
