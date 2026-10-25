package com.seleniumboot.unit;

import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Per-test recording of console entries and failed requests, as used by the trace. */
public class TraceCaptureMetricsTest {

    @Test
    public void consoleEntry_isStoredWithOffsetFromTestStart() {
        String id = "capture.console";
        ExecutionMetrics.markStart(id);
        long now = System.currentTimeMillis();
        ExecutionMetrics.recordConsoleEntry(id, "boom", now + 250);
        TestTiming t = ExecutionMetrics.getTiming(id);
        assertEquals(t.getConsoleEntries().size(), 1);
        long off = t.getConsoleEntries().get(0).getOffsetMs();
        assertTrue(off >= 200 && off <= 400, "offset was " + off);
    }

    @Test
    public void networkFailure_isStored_andCapped() {
        String id = "capture.network";
        ExecutionMetrics.markStart(id);
        for (int i = 0; i < 300; i++) ExecutionMetrics.recordNetworkFailure(id, "GET", "http://x/" + i, 404, null);
        assertEquals(ExecutionMetrics.getTiming(id).getNetworkFailures().size(), 200);
    }

    @Test
    public void clearSteps_alsoClearsTheCapturedEntries() {
        String id = "capture.clear";
        ExecutionMetrics.markStart(id);
        ExecutionMetrics.recordNetworkFailure(id, "GET", "http://x", 500, null);
        ExecutionMetrics.recordConsoleEntry(id, "e", System.currentTimeMillis());
        ExecutionMetrics.clearSteps(id);
        assertTrue(ExecutionMetrics.getTiming(id).getNetworkFailures().isEmpty());
        assertTrue(ExecutionMetrics.getTiming(id).getConsoleEntries().isEmpty());
    }

    @Test
    public void nullTestId_isIgnored() {
        ExecutionMetrics.recordNetworkFailure(null, "GET", "http://x", 500, null);
        ExecutionMetrics.recordConsoleEntry(null, "e", 1L);
    }
}
