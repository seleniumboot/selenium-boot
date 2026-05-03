package com.seleniumboot.cucumber;

import com.seleniumboot.browser.BrowserContext;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.client.ApiClient;
import com.seleniumboot.context.ScenarioContext;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.lifecycle.FrameworkBootstrap;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.network.NetworkMock;
import com.seleniumboot.reporting.ScreenshotManager;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import com.seleniumboot.testdata.TestDataStore;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Selenium Boot lifecycle hooks for Cucumber scenarios.
 *
 * <p>Auto-discovered by Cucumber when {@code "com.seleniumboot.cucumber"} is
 * included in {@code @CucumberOptions(glue = {...})}.
 *
 * <p>Lifecycle per scenario:
 * <ol>
 *   <li>{@code @Before(order=1000)} — bootstrap framework, create WebDriver, start timing.</li>
 *   <li>Cucumber steps run; step names logged via {@link CucumberStepLogger}.</li>
 *   <li>{@code @After(order=20000)} — screenshot on failure, record metrics, quit driver.</li>
 * </ol>
 *
 * <p>Order rationale:
 * <ul>
 *   <li>{@code @Before(order=1000)}: runs before user's {@code @Before(order=10000)},
 *       so the driver is ready when user hooks execute.</li>
 *   <li>{@code @After(order=20000)}: Cucumber runs higher-order {@code @After} first,
 *       so this runs before user's {@code @After(order=10000)},
 *       ensuring screenshot is captured while the page is still loaded.</li>
 * </ul>
 */
public class CucumberHooks {

    @Before(order = 1000)
    public void beforeScenario(Scenario scenario) {
        // 1. Ensure framework is bootstrapped (idempotent)
        FrameworkBootstrap.initialize();

        // 2. Derive a unique, readable testId for this scenario
        String testId = buildTestId(scenario);

        // 3. Store scenario on thread so BaseCucumberSteps.getScenario() works
        CucumberContext.setScenario(scenario);

        // 4. Register testId so StepLogger and ScreenshotManager resolve it
        SeleniumBootContext.setCurrentTestId(testId);

        // 5. Initialize metrics — detect retry when testId already exists
        if (ExecutionMetrics.getTiming(testId) != null) {
            ExecutionMetrics.recordRetry(testId);
        }
        ExecutionMetrics.clearSteps(testId);
        ExecutionMetrics.markStart(testId);
        ExecutionMetrics.recordTestClass(testId, featureTitle(scenario.getUri()));
        ExecutionMetrics.recordDescription(testId, scenario.getName());

        // 6. Create WebDriver (acquires session semaphore slot)
        DriverManager.createDriver();

        // 7. Notify plugins
        HookRegistry.onTestStart(testId);
    }

    @After(order = 20000)
    public void afterScenario(Scenario scenario) {
        String testId = SeleniumBootContext.getCurrentTestId();

        if (testId == null) {
            safeQuitDriver();
            CucumberContext.clear();
            return;
        }

        try {
            boolean failed = scenario.isFailed();
            String status = resolveStatus(scenario);

            if (failed) {
                // Capture screenshot into Selenium Boot HTML report
                try {
                    String path = ScreenshotManager.capture(sanitize(scenario.getName()));
                    ExecutionMetrics.recordScreenshot(testId, path);
                } catch (Exception ignored) {}

                // Attach screenshot bytes to Cucumber's own report (HTML/JSON)
                try {
                    String base64 = ScreenshotManager.captureAsBase64();
                    if (base64 != null) {
                        scenario.attach(Base64.getDecoder().decode(base64), "image/png", "Failure Screenshot");
                    }
                } catch (Exception ignored) {}

                HookRegistry.onTestFailure(testId, new RuntimeException("Scenario failed: " + scenario.getName()));
            }

            // Collect any JS console errors captured during the scenario
            if (ConsoleErrorCollector.isEnabled()) {
                List<String> errors = ConsoleErrorCollector.collect();
                errors.forEach(e -> StepLogger.step("[JS Error] " + e, StepStatus.WARN));
            }

            ExecutionMetrics.recordStatus(testId, status);
            ExecutionMetrics.markEnd(testId);
            HookRegistry.onTestEnd(testId, status);

        } finally {
            safeQuitDriver();
            ScenarioContext.clear();
            TestDataStore.clear();
            ApiClient.clearGlobalAuth();
            BrowserContext.clear();
            NetworkMock.cleanup();
            CucumberContext.clear();
            SeleniumBootContext.clearCurrentTestId();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a unique, readable testId.
     *
     * Format: {@code <feature-filename>#<scenario-name>[L<line>]}
     *
     * Examples:
     *   {@code login.feature#User logs in with valid credentials[L12]}
     *   {@code checkout.feature#Purchase as {string}[L34]}  ← outline example at line 34
     */
    static String buildTestId(Scenario scenario) {
        String feature = extractFileName(scenario.getUri());
        String name    = scenario.getName() != null ? scenario.getName() : "unnamed";
        int    line    = scenario.getLine() != null ? scenario.getLine() : 0;
        return feature + "#" + name + "[L" + line + "]";
    }

    private static String extractFileName(URI uri) {
        if (uri == null) return "unknown.feature";
        String path = uri.toString();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String featureTitle(URI uri) {
        String name = extractFileName(uri);
        return name.endsWith(".feature") ? name.substring(0, name.length() - 8) : name;
    }

    private static String resolveStatus(Scenario scenario) {
        if (scenario.isFailed()) return "FAILED";
        io.cucumber.java.Status s = scenario.getStatus();
        if (s == io.cucumber.java.Status.SKIPPED
                || s == io.cucumber.java.Status.PENDING
                || s == io.cucumber.java.Status.UNDEFINED) return "SKIPPED";
        return "PASSED";
    }

    private static String sanitize(String input) {
        return input == null ? "unnamed" : input.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
    }

    private void safeQuitDriver() {
        try {
            if (DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        } catch (Exception e) {
            System.err.println("[CucumberHooks] Driver quit failed: " + e.getMessage());
        }
    }
}
