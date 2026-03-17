package com.seleniumboot.precondition;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.test.BasePage;
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Base class for condition provider classes.
 *
 * <p>Extend this class and annotate methods with {@link ConditionProvider} to define
 * named pre-conditions. Register the class via Java SPI or programmatically:
 *
 * <pre>
 * // SPI — create META-INF/services/com.seleniumboot.precondition.BaseConditions
 * //       containing the fully-qualified class name of your subclass
 *
 * // Programmatic
 * PreConditionRegistry.register(new AppConditions());
 * </pre>
 *
 * <p>Example:
 * <pre>
 * public class AppConditions extends BaseConditions {
 *
 *     {@literal @}ConditionProvider("login")
 *     public void login() {
 *         open("/login");
 *         type(By.id("username"), "admin");
 *         type(By.id("password"), "secret");
 *         click(By.id("submit"));
 *         WaitEngine.waitForUrlContains("/dashboard");
 *     }
 * }
 * </pre>
 *
 * @see ConditionProvider
 * @see PreCondition
 * @since 0.8.0
 */
@SeleniumBootApi(since = "0.8.0")
public abstract class BaseConditions {

    /** Returns the framework-managed WebDriver for the calling thread. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /** Navigates to the configured {@code baseUrl}. */
    protected void open() {
        String baseUrl = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("baseUrl is not configured in selenium-boot.yml");
        }
        getDriver().get(baseUrl);
    }

    /** Navigates to {@code baseUrl + path}. */
    protected void open(String path) {
        String baseUrl = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("baseUrl is not configured in selenium-boot.yml");
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        getDriver().get(normalized + path);
    }

    /** Waits for the element to be clickable and clicks it. */
    protected void click(By locator) {
        WaitEngine.waitForClickable(locator).click();
    }

    /** Waits for the element to be visible, clears it, then types text. */
    protected void type(By locator, String text) {
        var el = WaitEngine.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }
}
