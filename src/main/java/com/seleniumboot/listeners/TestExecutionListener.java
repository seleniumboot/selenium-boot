package com.seleniumboot.listeners;

import com.seleniumboot.browser.BrowserContext;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.precondition.PreConditionRunner;
import com.seleniumboot.reporting.ScreenshotManager;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.List;

/**
 * TestExecutionListener manages the per-test method lifecycle within Selenium Boot.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Creates a WebDriver instance at the start of each test method.</li>
 *     <li>Ensures the WebDriver is terminated after test completion
 *         (success, failure, or skip).</li>
 *     <li>Captures failure artifacts (e.g., screenshots) before driver shutdown.</li>
 * </ul>
 *
 * <p>Design Principles:
 * <ul>
 *     <li>One test method = one WebDriver session.</li>
 *     <li>Thread-safe execution using ThreadLocal driver management.</li>
 *     <li>Deterministic cleanup to prevent session leaks under parallel load.</li>
 * </ul>
 *
 * <p>This listener does not manage suite-level initialization.
 * Global setup is handled by {@code SuiteExecutionListener}.
 */

public final class TestExecutionListener implements ITestListener {

    /** Tracks whether JS errors have already been logged for this test (prevents double-logging on failure redirect). */
    private static final ThreadLocal<Boolean> jsErrorsLogged = ThreadLocal.withInitial(() -> false);

    @Override
    public void onTestStart(ITestResult result) {
        String testId = result.getMethod().getQualifiedName();
        SeleniumBootContext.setCurrentTestId(testId);
        ExecutionMetrics.clearSteps(testId);   // discard stale steps from prior retry attempt
        ExecutionMetrics.markStart(testId);
        ExecutionMetrics.recordTestClass(testId, result.getTestClass().getRealClass().getSimpleName());
        ExecutionMetrics.recordDescription(testId, result.getMethod().getDescription());
        // Set browser override BEFORE creating driver so DriverProviderFactory can read it
        String browserOverride = result.getTestContext().getCurrentXmlTest()
                .getParameter("selenium.boot.browser");
        if (browserOverride != null && !browserOverride.isEmpty()) {
            BrowserContext.set(browserOverride);
            ExecutionMetrics.recordBrowser(testId, browserOverride);
        }
        DriverManager.createDriver();
        PreConditionRunner.run(result);
        loadTestData(result);
        HookRegistry.onTestStart(testId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = result.getMethod().getQualifiedName();

        if (ConsoleErrorCollector.isEnabled()) {
            List<String> errors = ConsoleErrorCollector.collect();
            errors.forEach(e -> StepLogger.step("[JS Error] " + e, StepStatus.WARN));
            jsErrorsLogged.set(true);

            boolean failOnErrors = false;
            try { failOnErrors = SeleniumBootContext.getConfig().getBrowser().isFailOnConsoleErrors(); } catch (Exception ignored) {}

            if (failOnErrors && !errors.isEmpty()) {
                result.setStatus(ITestResult.FAILURE);
                result.setThrowable(new AssertionError("JS console errors detected (" + errors.size() + "): " + errors));
                onTestFailure(result);
                return;
            }
        }

        ExecutionMetrics.recordStatus(testId, "PASSED");
        ExecutionMetrics.markEnd(testId);
        HookRegistry.onTestEnd(testId, "PASSED");
        if (DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        BrowserContext.clear();
        SeleniumBootContext.clearCurrentTestId();
        jsErrorsLogged.set(false);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String testId = result.getMethod().getQualifiedName();

        if (ConsoleErrorCollector.isEnabled() && !jsErrorsLogged.get()) {
            ConsoleErrorCollector.collect().forEach(e -> StepLogger.step("[JS Error] " + e, StepStatus.WARN));
        }
        jsErrorsLogged.set(false);

        ExecutionMetrics.recordStatus(testId, "FAILED");
        ExecutionMetrics.markEnd(testId);
        if (result.getThrowable() != null) {
            ExecutionMetrics.recordError(testId, result.getThrowable());
        }
        HookRegistry.onTestFailure(testId, result.getThrowable());
        String screenshotPath = ScreenshotManager.capture(testName);
        ExecutionMetrics.recordScreenshot(testId, screenshotPath);
        if (DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        BrowserContext.clear();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testId = result.getMethod().getQualifiedName();
        ExecutionMetrics.recordStatus(testId, "SKIPPED");
        ExecutionMetrics.markEnd(testId);
        HookRegistry.onTestEnd(testId, "SKIPPED");
        if (DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        BrowserContext.clear();
        SeleniumBootContext.clearCurrentTestId();
    }

    private void loadTestData(ITestResult result) {
        com.seleniumboot.testdata.TestData annotation =
                result.getMethod().getConstructorOrMethod().getMethod()
                      .getAnnotation(com.seleniumboot.testdata.TestData.class);
        if (annotation == null) {
            annotation = result.getTestClass().getRealClass()
                               .getAnnotation(com.seleniumboot.testdata.TestData.class);
        }
        if (annotation != null) {
            com.seleniumboot.testdata.TestDataStore.set(
                com.seleniumboot.testdata.TestDataLoader.load(annotation.value())
            );
        }
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }
}
