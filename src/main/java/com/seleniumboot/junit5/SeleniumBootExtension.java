package com.seleniumboot.junit5;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.lifecycle.FrameworkBootstrap;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.reporting.ScreenshotManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * JUnit 5 extension providing the same lifecycle management as
 * {@code SuiteExecutionListener} + {@code TestExecutionListener} do for TestNG.
 *
 * <p>Usage — annotate your test class or its base class:
 * <pre>{@code
 * @ExtendWith(SeleniumBootExtension.class)
 * class MyTest {
 *     WebDriver driver = DriverManager.getDriver();
 * }
 * }</pre>
 *
 * <p>Or use the composed {@link EnableSeleniumBoot} annotation:
 * <pre>{@code
 * @EnableSeleniumBoot
 * class MyTest { ... }
 * }</pre>
 *
 * <p>Lifecycle per test method:
 * <ol>
 *   <li>{@code beforeAll} — bootstraps framework once per JVM (idempotent)</li>
 *   <li>{@code beforeEach} — sets test ID, starts timing, creates WebDriver</li>
 *   <li>{@code testSuccessful / testFailed / testAborted} — records outcome,
 *       captures screenshot on failure, quits driver</li>
 * </ol>
 */
public class SeleniumBootExtension implements BeforeAllCallback, BeforeEachCallback, TestWatcher {

    @Override
    public void beforeAll(ExtensionContext context) {
        FrameworkBootstrap.initialize();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        String testId = buildTestId(context);
        SeleniumBootContext.setCurrentTestId(testId);
        ExecutionMetrics.markStart(testId);
        DriverManager.createDriver();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testId = buildTestId(context);
        ExecutionMetrics.recordStatus(testId, "PASSED");
        ExecutionMetrics.markEnd(testId);
        DriverManager.quitDriver();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testId = buildTestId(context);
        String testName = context.getRequiredTestMethod().getName();
        ExecutionMetrics.recordStatus(testId, "FAILED");
        ExecutionMetrics.markEnd(testId);
        String screenshotPath = ScreenshotManager.capture(testName);
        ExecutionMetrics.recordScreenshot(testId, screenshotPath);
        DriverManager.quitDriver();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String testId = buildTestId(context);
        ExecutionMetrics.recordStatus(testId, "SKIPPED");
        ExecutionMetrics.markEnd(testId);
        DriverManager.quitDriver();
        SeleniumBootContext.clearCurrentTestId();
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        // No driver is created for disabled tests — nothing to clean up
    }

    private static String buildTestId(ExtensionContext context) {
        return context.getRequiredTestClass().getName()
                + "#"
                + context.getRequiredTestMethod().getName();
    }
}
