package com.seleniumboot.listeners;

import com.seleniumboot.assertion.SoftAssertionCollector;
import com.seleniumboot.assertion.SoftAssertions;
import com.seleniumboot.context.ScenarioContext;
import com.seleniumboot.browser.BrowserContext;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.test.BaseApiTest;
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
        if (!isApiTest(result)) DriverManager.createDriver();
        PreConditionRunner.run(result);
        loadTestData(result);
        HookRegistry.onTestStart(testId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = result.getMethod().getQualifiedName();

        if (!isApiTest(result) && ConsoleErrorCollector.isEnabled()) {
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

        // Flush soft assertions — if any failed, redirect to failure path
        SoftAssertionCollector collector = SoftAssertions.get();
        if (collector.hasFailed()) {
            List<String> softFailures = collector.getFailures();
            // Log each failure as a step entry
            softFailures.forEach(msg ->
                StepLogger.step("[Soft Assertion Failed] " + msg, StepStatus.FAIL));
            // Single screenshot at flush time
            String screenshotPath = ScreenshotManager.capture(result.getMethod().getMethodName());
            ExecutionMetrics.recordScreenshot(result.getMethod().getQualifiedName(), screenshotPath);
            // Build combined error message
            String combined = softFailures.size() + " soft assertion(s) failed:\n" +
                String.join("\n", softFailures);
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(new AssertionError(combined));
            SoftAssertions.clear();
            onTestFailure(result);
            return;
        }
        SoftAssertions.clear();

        ExecutionMetrics.recordStatus(testId, "PASSED");
        ExecutionMetrics.markEnd(testId);
        HookRegistry.onTestEnd(testId, "PASSED");
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        BrowserContext.clear();
        SeleniumBootContext.clearCurrentTestId();
        jsErrorsLogged.set(false);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String testId = result.getMethod().getQualifiedName();

        if (!isApiTest(result) && ConsoleErrorCollector.isEnabled() && !jsErrorsLogged.get()) {
            ConsoleErrorCollector.collect().forEach(e -> StepLogger.step("[JS Error] " + e, StepStatus.WARN));
        }
        jsErrorsLogged.set(false);

        ExecutionMetrics.recordStatus(testId, "FAILED");
        ExecutionMetrics.markEnd(testId);
        if (result.getThrowable() != null) {
            ExecutionMetrics.recordError(testId, result.getThrowable());
        }
        HookRegistry.onTestFailure(testId, result.getThrowable());
        String screenshotPath = isApiTest(result) ? null : ScreenshotManager.capture(testName);
        ExecutionMetrics.recordScreenshot(testId, screenshotPath);
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        BrowserContext.clear();
        SoftAssertions.clear();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testId = result.getMethod().getQualifiedName();
        ExecutionMetrics.recordStatus(testId, "SKIPPED");
        ExecutionMetrics.markEnd(testId);
        HookRegistry.onTestEnd(testId, "SKIPPED");
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        BrowserContext.clear();
        SoftAssertions.clear();
        SeleniumBootContext.clearCurrentTestId();
    }

    private boolean isApiTest(ITestResult result) {
        return BaseApiTest.class.isAssignableFrom(result.getTestClass().getRealClass());
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
