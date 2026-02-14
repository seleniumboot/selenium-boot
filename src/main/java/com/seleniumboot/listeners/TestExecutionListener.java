package com.seleniumboot.listeners;

import com.seleniumboot.driver.DriverManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

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

    @Override
    public void onTestStart(ITestResult result) {
        DriverManager.createDriver();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        DriverManager.quitDriver();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        com.seleniumboot.reporting.ScreenshotManager.capture(testName);
        DriverManager.quitDriver();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        DriverManager.quitDriver();
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }
}
