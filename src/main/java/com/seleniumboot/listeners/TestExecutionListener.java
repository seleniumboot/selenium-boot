package com.seleniumboot.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestExecutionListener controls per-test execution lifecycle.
 *
 * Responsibilities (MVP):
 * - Hook into test start / finish
 * - Provide safe extension points for:
 *   - Driver provisioning (future)
 *   - Failure capture (future)
 *   - Reporting hooks (future)
 *
 * This listener must remain lightweight and thread-safe.
 */
public final class TestExecutionListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        // Reserved for:
        // - ThreadLocal driver binding
        // - Test-level context initialization
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Reserved for:
        // - Reporting success
        // - Execution metadata capture
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Reserved for:
        // - Screenshot capture
        // - Failure metadata collection
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // Reserved for:
        // - Skip reason reporting
    }

    @Override
    public void onStart(ITestContext context) {
        // Test context initialized
        // Do NOT create WebDriver here
    }

    @Override
    public void onFinish(ITestContext context) {
        // Reserved for:
        // - Per-context cleanup
        // - Reporting flush hooks
    }
}
