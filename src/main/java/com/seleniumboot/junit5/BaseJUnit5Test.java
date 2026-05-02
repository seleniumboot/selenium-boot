package com.seleniumboot.junit5;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.assertion.LocatorAssert;
import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Optional base class for JUnit 5 tests — the JUnit 5 equivalent of {@code BaseTest}.
 *
 * <p>Extend this class to get the same convenience API available in TestNG {@code BaseTest}:
 * <pre>
 * class LoginTest extends BaseJUnit5Test {
 *
 *     {@literal @}Test
 *     void validLogin() {
 *         open();
 *         $("input#username").type("admin");
 *         $("input#password").type("secret");
 *         $("button[type='submit']").click();
 *         assertThat(By.id("dashboard")).isVisible();
 *     }
 * }
 * </pre>
 *
 * <p>Alternatively use {@link EnableSeleniumBoot} on your own base class and inject
 * {@code WebDriver} as a test method parameter.
 */
@SeleniumBootApi(since = "1.9.0")
@ExtendWith(SeleniumBootExtension.class)
public abstract class BaseJUnit5Test {

    /** Returns the WebDriver for the current thread. Valid inside test methods only. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /** Returns a {@link WebDriverWait} using the explicit timeout from {@code selenium-boot.yml}. */
    protected WebDriverWait getWait() {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
    }

    /** Navigates to {@code execution.baseUrl} from {@code selenium-boot.yml}. */
    protected void open() {
        String url = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        getDriver().get(url);
        if (ConsoleErrorCollector.isEnabled()) ConsoleErrorCollector.injectShim();
    }

    /** Navigates to {@code baseUrl + path}. */
    protected void open(String path) {
        String base = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        String sep  = base.endsWith("/") || path.startsWith("/") ? "" : "/";
        getDriver().get(base + sep + path);
        if (ConsoleErrorCollector.isEnabled()) ConsoleErrorCollector.injectShim();
    }

    /** Fluent chainable locator from a CSS selector. */
    protected Locator $(String css) {
        return Locator.ofCss(css);
    }

    /** Fluent chainable locator from a Selenium {@link By}. */
    protected Locator $(By by) {
        return Locator.of(by);
    }

    /** Auto-retrying assertion on a {@link By} locator. */
    protected LocatorAssert assertThat(By locator) {
        return SeleniumAssert.assertThat(locator);
    }

    /** Auto-retrying assertion on a {@link Locator} chain. */
    protected LocatorAssert assertThat(Locator locator) {
        return SeleniumAssert.assertThat(locator);
    }

    /** Logs a named step into the HTML report step timeline. */
    protected void step(String name) {
        StepLogger.step(name);
    }

    /** Logs a named step with a screenshot into the HTML report step timeline. */
    protected void step(String name, boolean screenshot) {
        StepLogger.step(name, screenshot);
    }

    /** Logs a named step with an explicit status. */
    protected void step(String name, StepStatus status) {
        StepLogger.step(name, status);
    }
}
