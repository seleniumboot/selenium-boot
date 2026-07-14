package com.seleniumboot.unit;

import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.reporting.ScreenshotManager;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepRecord;
import com.seleniumboot.steps.StepStatus;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link StepLogger}.
 *
 * <p>All collaborators ({@link SeleniumBootContext}, {@link ExecutionMetrics},
 * {@link ScreenshotManager}) are static, so they are mocked with
 * {@code mockStatic} — no real browser or test run required.
 */
public class StepLoggerTest {

    private static final String TEST_ID = "com.example.LoginTest.login";

    private MockedStatic<SeleniumBootContext> contextMock;
    private MockedStatic<ExecutionMetrics> metricsMock;
    private MockedStatic<ScreenshotManager> screenshotMock;

    @BeforeMethod
    public void setup() {
        contextMock = mockStatic(SeleniumBootContext.class);
        metricsMock = mockStatic(ExecutionMetrics.class);
        screenshotMock = mockStatic(ScreenshotManager.class);
    }

    @AfterMethod
    public void teardown() {
        contextMock.close();
        metricsMock.close();
        screenshotMock.close();
    }

    // ── No active test context → step is ignored ───────────────────────────────

    @Test
    public void step_isIgnored_whenNoActiveTestContext() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(null);

        StepLogger.step("orphan step");

        // Nothing recorded, no screenshot attempted
        metricsMock.verify(() -> ExecutionMetrics.recordStep(any(), any()), never());
        screenshotMock.verify(ScreenshotManager::captureAsBase64, never());
    }

    // ── Basic step: INFO status, no screenshot ─────────────────────────────────

    @Test
    public void step_recordsInfoStatus_withoutScreenshot() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(TEST_ID);
        metricsMock.when(() -> ExecutionMetrics.getTestStartTime(TEST_ID))
                .thenReturn(System.currentTimeMillis() - 1000);

        StepLogger.step("Navigate to login page");

        StepRecord record = captureRecordedStep();
        assertEquals(record.getName(), "Navigate to login page");
        assertEquals(record.getStatus(), StepStatus.INFO.name());
        assertNull(record.getScreenshotBase64(), "no screenshot expected");
        assertTrue(record.getOffsetMs() >= 0, "offset should be computed from start time");
        screenshotMock.verify(ScreenshotManager::captureAsBase64, never());
    }

    // ── Screenshot requested → captured and attached ───────────────────────────

    @Test
    public void step_capturesScreenshot_whenRequested() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(TEST_ID);
        metricsMock.when(() -> ExecutionMetrics.getTestStartTime(TEST_ID)).thenReturn(0L);
        screenshotMock.when(ScreenshotManager::captureAsBase64).thenReturn("BASE64DATA");

        StepLogger.step("After credentials entered", true);

        StepRecord record = captureRecordedStep();
        assertEquals(record.getScreenshotBase64(), "BASE64DATA");
        screenshotMock.verify(ScreenshotManager::captureAsBase64, times(1));
    }

    // ── Explicit status is preserved ───────────────────────────────────────────

    @Test
    public void step_recordsExplicitStatus() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(TEST_ID);
        metricsMock.when(() -> ExecutionMetrics.getTestStartTime(TEST_ID))
                .thenReturn(System.currentTimeMillis());

        StepLogger.step("Verify dashboard visible", StepStatus.PASS);

        assertEquals(captureRecordedStep().getStatus(), StepStatus.PASS.name());
    }

    // ── offsetMs is 0 when the start time is unknown ───────────────────────────

    @Test
    public void step_offsetIsZero_whenStartTimeUnknown() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(TEST_ID);
        metricsMock.when(() -> ExecutionMetrics.getTestStartTime(TEST_ID)).thenReturn(0L);

        StepLogger.step("no start time");

        assertEquals(captureRecordedStep().getOffsetMs(), 0L);
    }

    // ── stepWithScreenshot: pre-captured base64 is stored as-is ────────────────

    @Test
    public void stepWithScreenshot_storesProvidedBase64() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(TEST_ID);
        metricsMock.when(() -> ExecutionMetrics.getTestStartTime(TEST_ID))
                .thenReturn(System.currentTimeMillis());

        StepLogger.stepWithScreenshot("Visual diff", StepStatus.FAIL, "DIFFIMG");

        StepRecord record = captureRecordedStep();
        assertEquals(record.getScreenshotBase64(), "DIFFIMG");
        assertEquals(record.getStatus(), StepStatus.FAIL.name());
        // stepWithScreenshot never triggers a live capture
        screenshotMock.verify(ScreenshotManager::captureAsBase64, never());
    }

    @Test
    public void stepWithScreenshot_isIgnored_whenNoActiveTestContext() {
        contextMock.when(SeleniumBootContext::getCurrentTestId).thenReturn(null);

        StepLogger.stepWithScreenshot("orphan", StepStatus.INFO, "IMG");

        metricsMock.verify(() -> ExecutionMetrics.recordStep(any(), any()), never());
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    private StepRecord captureRecordedStep() {
        ArgumentCaptor<StepRecord> captor = ArgumentCaptor.forClass(StepRecord.class);
        metricsMock.verify(() -> ExecutionMetrics.recordStep(eq(TEST_ID), captor.capture()));
        return captor.getValue();
    }
}
