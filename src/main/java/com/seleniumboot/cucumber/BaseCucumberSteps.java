package com.seleniumboot.cucumber;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.assertion.LocatorAssert;
import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import io.cucumber.java.Scenario;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Abstract base class for Cucumber step definition classes.
 *
 * <p>Provides the same driver-access conveniences as {@code BaseTest}:
 * <pre>
 * public class LoginSteps extends BaseCucumberSteps {
 *
 *     {@literal @}Given("the user is on the login page")
 *     public void onLoginPage() { open(); }
 *
 *     {@literal @}When("they login as {string}")
 *     public void login(String username) {
 *         new LoginPage(getDriver()).login(username, "secret");
 *     }
 *
 *     {@literal @}Then("the dashboard is visible")
 *     public void dashboardVisible() {
 *         assertThat(By.id("dashboard")).isVisible();
 *     }
 * }
 * </pre>
 *
 * <p>Thread-safe: all state access goes through {@link DriverManager} and
 * {@link CucumberContext} ThreadLocals.
 */
@SeleniumBootApi(since = "1.9.0")
public abstract class BaseCucumberSteps {

    /** Returns the WebDriver for the current thread. */
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
        getDriver().get(baseUrl());
        if (ConsoleErrorCollector.isEnabled()) ConsoleErrorCollector.injectShim();
    }

    /** Navigates to {@code baseUrl + path}. */
    protected void open(String path) {
        String base = baseUrl();
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        getDriver().get(normalized + path);
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

    /**
     * Returns the current Cucumber {@link Scenario} — useful for attaching
     * data or reading tags inside a step.
     */
    protected Scenario getScenario() {
        Scenario scenario = CucumberContext.getScenario();
        if (scenario == null) {
            throw new IllegalStateException(
                "[BaseCucumberSteps] No active scenario on this thread. " +
                "Ensure 'com.seleniumboot.cucumber' is in the @CucumberOptions glue.");
        }
        return scenario;
    }

    private String baseUrl() {
        String url = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException(
                "[BaseCucumberSteps] execution.baseUrl is not set in selenium-boot.yml");
        }
        return url;
    }
}
