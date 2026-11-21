package com.seleniumboot.unit;

import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.mockito.MockedStatic;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class PageAssertBehaviorTest {

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
    public void titleAssertionsSupportExactContainsAndRegex() {
        when(driver.getTitle()).thenReturn("Order 412 | Shop");

        SeleniumAssert.assertThatPage()
                .hasTitle("Order 412 | Shop")
                .titleContains("Order 412")
                .hasTitle(Pattern.compile("Order \\d+ \\| Shop"));
    }

    @Test
    public void urlAssertionsSupportExactContainsAndRegex() {
        when(driver.getCurrentUrl()).thenReturn("https://shop.example/orders/412");

        SeleniumAssert.assertThatPage()
                .hasUrl("https://shop.example/orders/412")
                .urlContains("/orders/")
                .hasUrl(Pattern.compile("https://shop\\.example/orders/\\d+"));
    }

    @Test
    public void titleAssertionRetriesUntilNavigationFinishes() {
        when(driver.getTitle()).thenReturn("Loading…", "Loading…", "Dashboard");

        SeleniumAssert.assertThatPage().hasTitle("Dashboard");

        verify(driver, atLeast(3)).getTitle();
    }

    @Test
    public void urlAssertionRetriesThroughRedirect() {
        when(driver.getCurrentUrl()).thenReturn("https://shop.example/login", "https://shop.example/home");

        SeleniumAssert.assertThatPage().urlContains("/home");

        verify(driver, atLeast(2)).getCurrentUrl();
    }

    @Test
    public void regexIsAFullMatchNotAFind() {
        when(driver.getCurrentUrl()).thenReturn("https://shop.example/orders/412?tab=items");

        expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThatPage().hasUrl(Pattern.compile("https://shop\\.example/orders/\\d+")));
    }

    @Test
    public void titleFailureNamesExpectedAndActualTitle() {
        when(driver.getTitle()).thenReturn("Sign in");

        AssertionError error = expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThatPage().hasTitle("Dashboard"));

        assertTrue(error.getMessage().contains("Expected page title [Dashboard]"), error.getMessage());
        assertTrue(error.getMessage().contains("actual: [Sign in]"), error.getMessage());
        assertTrue(error.getMessage().contains("timeout: 1s"), error.getMessage());
    }

    @Test
    public void urlFailureNamesActualUrl() {
        when(driver.getCurrentUrl()).thenReturn("https://shop.example/login");

        AssertionError error = expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThatPage().urlContains("/dashboard"));

        assertTrue(error.getMessage().contains("Expected URL to contain [/dashboard]"), error.getMessage());
        assertTrue(error.getMessage().contains("actual: [https://shop.example/login]"), error.getMessage());
    }

    @Test
    public void nullTitleFailsAndIsReportedAsNull() {
        when(driver.getTitle()).thenReturn(null);

        AssertionError error = expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThatPage().titleContains("x"));

        assertTrue(error.getMessage().contains("actual: [null]"), error.getMessage());
    }
}
