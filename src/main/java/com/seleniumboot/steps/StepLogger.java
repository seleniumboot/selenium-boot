package com.seleniumboot.steps;

import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.reporting.ScreenshotManager;

/**
 * StepLogger allows test authors to log named steps during test execution.
 *
 * <p>Steps appear in the HTML report as a timeline inside each test's detail panel.
 * Screenshots can be captured at any step by passing {@code true} or {@link StepStatus}.
 *
 * <p>Usage:
 * <pre>
 *   StepLogger.step("Navigate to login page");
 *   StepLogger.step("After credentials entered", true);     // with screenshot
 *   StepLogger.step("Verify dashboard visible", StepStatus.PASS);
 * </pre>
 *
 * <p>Thread-safe — safe to use in parallel test execution.
 */
public final class StepLogger {

    private StepLogger() {}

    /** Log a step with INFO status and no screenshot. */
    public static void step(String name) {
        log(name, StepStatus.INFO, false);
    }

    /** Log a step with INFO status; optionally capture a screenshot. */
    public static void step(String name, boolean screenshot) {
        log(name, StepStatus.INFO, screenshot);
    }

    /** Log a step with an explicit status and no screenshot. */
    public static void step(String name, StepStatus status) {
        log(name, status, false);
    }

    /** Log a step with an explicit status; optionally capture a screenshot. */
    public static void step(String name, StepStatus status, boolean screenshot) {
        log(name, status, screenshot);
    }

    // ------------------------------------------------------------------

    private static void log(String name, StepStatus status, boolean screenshot) {
        String testId = SeleniumBootContext.getCurrentTestId();
        if (testId == null) {
            System.err.println("[StepLogger] No active test context — step ignored: " + name);
            return;
        }

        long startTime = ExecutionMetrics.getTestStartTime(testId);
        long offsetMs  = startTime > 0 ? System.currentTimeMillis() - startTime : 0L;

        String base64 = null;
        if (screenshot) {
            base64 = ScreenshotManager.captureAsBase64();
        }

        ExecutionMetrics.recordStep(testId, new StepRecord(name, offsetMs, status.name(), base64));
        System.out.printf("[StepLogger] [%-4s] +%dms  %s%n", status.name(), offsetMs, name);
    }
}
