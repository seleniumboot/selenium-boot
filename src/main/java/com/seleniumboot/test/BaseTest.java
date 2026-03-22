package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.listeners.SuiteExecutionListener;
import com.seleniumboot.listeners.TestExecutionListener;
import com.seleniumboot.testdata.TestDataStore;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import java.util.Map;

/**
 * BaseTest is the mandatory superclass for all Selenium Boot tests.
 *
 * Responsibilities:
 * - Provide access to the framework-managed WebDriver
 *
 * Rules:
 * - Tests must NOT create or quit WebDriver
 * - Tests must NOT manage waits or retries
 */
@SeleniumBootApi(since = "0.1.0")
@Listeners({
        SuiteExecutionListener.class,
        TestExecutionListener.class
})
public abstract class BaseTest {

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    protected void open() {
        String baseURL = SeleniumBootContext.getConfig()
                .getExecution().getBaseUrl();

        if (baseURL == null || baseURL.isEmpty()) {
            throw new IllegalStateException("baseURL is null or empty");
        }
        getDriver().get(baseURL);
        if (ConsoleErrorCollector.isEnabled()) ConsoleErrorCollector.injectShim();
    }

    protected void open(String path) {
        String baseUrl = SeleniumBootContext.getConfig()
                .getExecution().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("baseURL is null or empty");
        }

        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        String fullUrl = normalized + path;
        getDriver().get(fullUrl);
        if (ConsoleErrorCollector.isEnabled()) ConsoleErrorCollector.injectShim();
    }

    /**
     * Returns the test data loaded by {@code @TestData} for the current test.
     * Returns an empty map if no {@code @TestData} annotation was declared.
     *
     * <pre>
     * Map&lt;String, Object&gt; data = getTestData();
     * String username = (String) data.get("username");
     * </pre>
     */
    protected Map<String, Object> getTestData() {
        return TestDataStore.get();
    }
}
