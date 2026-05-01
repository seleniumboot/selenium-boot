package com.seleniumboot.listeners;

import com.seleniumboot.assertion.SoftAssertionCollector;
import com.seleniumboot.assertion.SoftAssertions;
import com.seleniumboot.browser.BrowserContext;
import com.seleniumboot.browser.ConsoleErrorCollector;
import com.seleniumboot.client.ApiAuth;
import com.seleniumboot.client.ApiClient;
import com.seleniumboot.client.UseAuth;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.context.ScenarioContext;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.ai.AiFailureAnalyzer;
import com.seleniumboot.network.NetworkMock;
import com.seleniumboot.precondition.ApiHealthChecker;
import com.seleniumboot.tracing.TraceRecorder;
import com.seleniumboot.precondition.DependsOnApi;
import com.seleniumboot.precondition.PreConditionRunner;
import com.seleniumboot.recording.RecordingManager;
import com.seleniumboot.reporting.ScreenshotManager;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import com.seleniumboot.test.BaseApiTest;
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
        if (isCucumberScenario(result)) return;
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
        // API health checks — skip immediately if a dependency is down,
        // before creating a browser session so no resources are wasted.
        checkApiDependencies(result);

        if (!isApiTest(result)) {
            DriverManager.createDriver();
            startRecordingIfEnabled();
        }
        applyUseAuth(result);
        PreConditionRunner.run(result);
        loadTestData(result);
        HookRegistry.onTestStart(testId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (isCucumberScenario(result)) return;
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

        RecordingManager.stop(); // discard frames — test passed
        ExecutionMetrics.recordStatus(testId, "PASSED");
        ExecutionMetrics.markEnd(testId);
        saveTraceIfEnabled(testId, result.getMethod().getMethodName(), true);
        HookRegistry.onTestEnd(testId, "PASSED");
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        com.seleniumboot.client.ApiClient.clearGlobalAuth();
        BrowserContext.clear();
        NetworkMock.cleanup();
        SeleniumBootContext.clearCurrentTestId();
        jsErrorsLogged.set(false);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (isCucumberScenario(result)) return;
        String testName = result.getMethod().getMethodName();
        String testId = result.getMethod().getQualifiedName();

        if (!isApiTest(result) && ConsoleErrorCollector.isEnabled() && !jsErrorsLogged.get()) {
            ConsoleErrorCollector.collect().forEach(e -> StepLogger.step("[JS Error] " + e, StepStatus.WARN));
        }
        jsErrorsLogged.set(false);

        String recordingPath = isApiTest(result) ? null : RecordingManager.saveOnFailure(testId);
        ExecutionMetrics.recordRecording(testId, recordingPath);
        ExecutionMetrics.recordStatus(testId, "FAILED");
        ExecutionMetrics.markEnd(testId);
        if (result.getThrowable() != null) {
            ExecutionMetrics.recordError(testId, result.getThrowable());
        }
        saveTraceIfEnabled(testId, result.getMethod().getMethodName(), false);
        runAiAnalysisIfEnabled(testId);
        HookRegistry.onTestFailure(testId, result.getThrowable());
        String screenshotPath = isApiTest(result) ? null : ScreenshotManager.capture(testName);
        ExecutionMetrics.recordScreenshot(testId, screenshotPath);
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        com.seleniumboot.client.ApiClient.clearGlobalAuth();
        BrowserContext.clear();
        SoftAssertions.clear();
        NetworkMock.cleanup();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (isCucumberScenario(result)) return;
        String testId = result.getMethod().getQualifiedName();
        ExecutionMetrics.recordStatus(testId, "SKIPPED");
        ExecutionMetrics.markEnd(testId);
        HookRegistry.onTestEnd(testId, "SKIPPED");
        if (!isApiTest(result) && DriverManager.shouldQuitAfterTest()) DriverManager.quitDriver();
        com.seleniumboot.testdata.TestDataStore.clear();
        ScenarioContext.clear();
        com.seleniumboot.client.ApiClient.clearGlobalAuth();
        BrowserContext.clear();
        SoftAssertions.clear();
        SeleniumBootContext.clearCurrentTestId();
    }

    private void checkApiDependencies(ITestResult result) {
        java.lang.reflect.Method method = result.getMethod().getConstructorOrMethod().getMethod();
        DependsOnApi[] methodLevel = method.getAnnotationsByType(DependsOnApi.class);
        DependsOnApi[] classLevel  = result.getTestClass().getRealClass().getAnnotationsByType(DependsOnApi.class);

        // Method-level takes precedence; fall back to class-level
        DependsOnApi[] deps = methodLevel.length > 0 ? methodLevel : classLevel;
        for (DependsOnApi dep : deps) {
            ApiHealthChecker.checkOrSkip(dep.value(), dep.timeoutSeconds());
        }
    }

    private void startRecordingIfEnabled() {
        try {
            com.seleniumboot.config.SeleniumBootConfig.Recording rec =
                    SeleniumBootContext.getConfig().getRecording();
            if (rec == null || !rec.isEnabled()) return;
            org.openqa.selenium.WebDriver driver = DriverManager.getDriver();
            if (driver == null) return;
            RecordingManager.start(driver, rec.getFps(), rec.getMaxDurationSeconds());
        } catch (Exception ignored) {}
    }

    private boolean isApiTest(ITestResult result) {
        return BaseApiTest.class.isAssignableFrom(result.getTestClass().getRealClass());
    }

    private void applyUseAuth(ITestResult result) {
        UseAuth annotation = result.getMethod().getConstructorOrMethod().getMethod()
                .getAnnotation(UseAuth.class);
        if (annotation == null) {
            annotation = result.getTestClass().getRealClass().getAnnotation(UseAuth.class);
        }
        if (annotation == null) return;

        String strategyName = annotation.value();
        try {
            SeleniumBootConfig.Api api = SeleniumBootContext.getConfig().getApi();
            if (api == null || api.getAuth() == null) return;
            SeleniumBootConfig.Api.AuthStrategy strategy = api.getAuth().get(strategyName);
            if (strategy == null) {
                throw new IllegalStateException("[UseAuth] No auth strategy named '" + strategyName + "' found in api.auth config");
            }
            ApiAuth auth = resolveAuthStrategy(strategy);
            if (auth != null) ApiClient.setGlobalAuth(auth);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[UseAuth] Failed to apply auth strategy '" + strategyName + "'", e);
        }
    }

    private ApiAuth resolveAuthStrategy(SeleniumBootConfig.Api.AuthStrategy s) {
        String type = s.getType();
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "bearer": return ApiAuth.bearerToken(resolveEnvVar(s.getToken()));
            case "basic":  return ApiAuth.basicAuth(resolveEnvVar(s.getUsername()), resolveEnvVar(s.getPassword()));
            case "oauth2": return ApiAuth.oauth2(resolveEnvVar(s.getTokenUrl()),
                                                 resolveEnvVar(s.getClientId()),
                                                 resolveEnvVar(s.getClientSecret()));
            default: throw new IllegalArgumentException("[UseAuth] Unknown auth type: '" + type + "'. Use bearer, basic, or oauth2");
        }
    }

    private String resolveEnvVar(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String varName = value.substring(2, value.length() - 1);
            String resolved = System.getenv(varName);
            if (resolved == null) resolved = System.getProperty(varName);
            return resolved != null ? resolved : value;
        }
        return value;
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

    private void runAiAnalysisIfEnabled(String testId) {
        try {
            String pageUrl   = null;
            String pageTitle = null;
            try {
                org.openqa.selenium.WebDriver driver = DriverManager.getDriver();
                if (driver != null) {
                    pageUrl   = driver.getCurrentUrl();
                    pageTitle = driver.getTitle();
                }
            } catch (Exception ignored) {}
            AiFailureAnalyzer.analyze(testId, pageUrl, pageTitle);
        } catch (Exception ignored) {}
    }

    private void saveTraceIfEnabled(String testId, String testName, boolean isPassing) {
        try {
            SeleniumBootConfig.Tracing tracing = SeleniumBootContext.getConfig().getTracing();
            if (tracing == null || !tracing.isEnabled()) return;
            if (isPassing && !tracing.isCaptureOnPass()) return;
            TraceRecorder.save(testId, testName);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStart(ITestContext context) {
    }

    /**
     * Returns true when the result represents a Cucumber scenario execution
     * (AbstractTestNGCucumberTests#runScenario). CucumberHooks owns the full
     * lifecycle for those tests — this listener must be a no-op to avoid
     * duplicate entries in ExecutionMetrics.
     */
    private boolean isCucumberScenario(ITestResult result) {
        if (!"runScenario".equals(result.getMethod().getMethodName())) return false;
        try {
            Class<?> base = Class.forName(
                "io.cucumber.testng.AbstractTestNGCucumberTests",
                false,
                result.getTestClass().getRealClass().getClassLoader()
            );
            return base.isAssignableFrom(result.getTestClass().getRealClass());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public void onFinish(ITestContext context) {
    }
}
