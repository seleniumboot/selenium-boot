package com.seleniumboot.listeners;

import com.seleniumboot.driver.DriverManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestExecutionListener controls per-test execution lifecycle.
 *
 * Responsibilities (MVP):
 * - Create WebDriver before test execution
 * - Quit WebDriver after test execution
 * - Provide hooks for failure handling (future)
 */
public final class TestExecutionListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        // Create WebDriver for current thread
        DriverManager.createDriver();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Reserved for reporting hooks
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Reserved for:
        // - Screenshot capture
        // - Failure metadata
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // No-op for MVP
    }

    @Override
    public void onStart(ITestContext context) {
        // Do NOT create WebDriver here
        // Context-level initialization only
    }

    @Override
    public void onFinish(ITestContext context) {
        // Quit WebDriver bound to current thread
        DriverManager.quitDriver();
    }
}
