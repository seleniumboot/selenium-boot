package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.assertion.LocatorAssert;
import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.assertion.SoftAssertionCollector;
import com.seleniumboot.assertion.SoftAssertions;
import com.seleniumboot.browser.ClipboardHelper;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.browser.DeviceEmulator;
import com.seleniumboot.browser.GeoLocation;
import com.seleniumboot.browser.StorageHelper;
import com.seleniumboot.visual.VisualAssert;
import com.seleniumboot.visual.VisualTolerance;
import com.seleniumboot.client.ApiClient;
import com.seleniumboot.context.ScenarioContext;
import com.seleniumboot.context.SuiteContext;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.listeners.SuiteExecutionListener;
import com.seleniumboot.listeners.TestExecutionListener;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.network.NetworkMock;
import com.seleniumboot.testdata.TestDataStore;
import org.openqa.selenium.By;
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

    /**
     * Returns the soft assertion collector for this test.
     * Failures collected here are reported all-at-once at test end
     * without interrupting test execution.
     *
     * <pre>
     * softAssert().that(pageTitle.equals("Home"), "Expected title 'Home', was: " + pageTitle);
     * softAssert().that(isLoggedIn, "User should be logged in after login flow");
     * </pre>
     */
    protected SoftAssertionCollector softAssert() {
        return SoftAssertions.get();
    }

    /** API client for hybrid UI+API tests. */
    protected ApiClient apiClient() {
        return ApiClient.create();
    }

    /** In-test thread-local context store. Cleared after each test. */
    protected ScenarioContext ctx() {
        return ScenarioContextHolder.INSTANCE;
    }

    /** Suite-scoped global context store. Survives between tests. */
    protected SuiteContext suiteCtx() {
        return SuiteContextHolder.INSTANCE;
    }

    // ----------------------------------------------------------
    // Phase 14 — Network, Storage, GeoLocation, Clipboard
    // ----------------------------------------------------------

    /** Network interception — stub API responses via CDP. */
    protected NetworkMock networkMock() {
        return NetworkMock.get();
    }

    /** localStorage read/write helpers. */
    protected StorageHelper.LocalStorage localStorage() {
        return StorageHelper.localStorage();
    }

    /** sessionStorage read/write helpers. */
    protected StorageHelper.SessionStorage sessionStorage() {
        return StorageHelper.sessionStorage();
    }

    /** Cookie read/write helpers. */
    protected StorageHelper.Cookies cookies() {
        return StorageHelper.cookies();
    }

    /** Geolocation mock — override browser location via CDP or JS. */
    protected GeoLocation mockLocation() {
        return GeoLocation.instance();
    }

    /** Clipboard read/write helpers. */
    protected ClipboardHelper clipboard() {
        return ClipboardHelper.instance();
    }

    // ----------------------------------------------------------
    // Fluent Locator API  ($)
    // ----------------------------------------------------------

    /** Creates a chainable {@link Locator} from a CSS selector. */
    protected Locator $(String css) {
        return Locator.ofCss(css);
    }

    /** Creates a chainable {@link Locator} from a Selenium {@link By} locator. */
    protected Locator $(By by) {
        return Locator.of(by);
    }

    // ----------------------------------------------------------
    // Web-First Assertions  (assertThat)
    // ----------------------------------------------------------

    /** Begins a fluent, auto-retrying assertion on the given locator. */
    protected LocatorAssert assertThat(By locator) {
        return SeleniumAssert.assertThat(locator);
    }

    /** Begins a fluent, auto-retrying assertion on the given {@link Locator} chain. */
    protected LocatorAssert assertThat(Locator locator) {
        return SeleniumAssert.assertThat(locator);
    }

    // ----------------------------------------------------------
    // Phase 15 — Visual Regression + Device Emulation
    // ----------------------------------------------------------

    /** Full-page screenshot comparison against the stored baseline. */
    protected void assertScreenshot(String name) {
        VisualAssert.assertScreenshot(name);
    }

    /** Full-page screenshot comparison with a custom pixel-difference tolerance. */
    protected void assertScreenshot(String name, VisualTolerance tolerance) {
        VisualAssert.assertScreenshot(name, tolerance);
    }

    /** Element-scoped screenshot comparison against the stored baseline. */
    protected void assertScreenshot(String name, By region) {
        VisualAssert.assertScreenshot(name, region);
    }

    /** Element-scoped screenshot comparison with a custom tolerance. */
    protected void assertScreenshot(String name, By region, VisualTolerance tolerance) {
        VisualAssert.assertScreenshot(name, region, tolerance);
    }

    /** Applies a named device profile (e.g. {@code "iPhone 14"}) to the current browser session. */
    protected void emulateDevice(String deviceName) {
        DeviceEmulator.emulate(deviceName);
    }

    /** Resets device emulation (restores desktop viewport and default user-agent). */
    protected void resetDevice() {
        DeviceEmulator.reset();
    }

    private static final class ScenarioContextHolder {
        static final ScenarioContext INSTANCE = new ScenarioContext();
    }
    private static final class SuiteContextHolder {
        static final SuiteContext INSTANCE = new SuiteContext();
    }
}
