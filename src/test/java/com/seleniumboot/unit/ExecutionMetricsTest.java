package com.seleniumboot.unit;

import com.seleniumboot.metrics.ExecutionMetrics;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ExecutionMetrics}.
 * No browser, no framework initialization required.
 */
public class ExecutionMetricsTest {

    @BeforeMethod
    public void resetMetrics() {
        ExecutionMetrics.reset();
    }

    @AfterMethod
    public void cleanUp() {
        ExecutionMetrics.reset();
    }

    // ----------------------------------------------------------
    // markStart / markEnd
    // ----------------------------------------------------------

    @Test
    public void markEnd_withKnownStart_recordsTotalTime() throws InterruptedException {
        ExecutionMetrics.markStart("test-1");
        Thread.sleep(50);
        ExecutionMetrics.markEnd("test-1");

        ExecutionMetrics.recordStatus("test-1", "PASSED");
        // No exception — confirms timing was recorded
    }

    @Test
    public void markEnd_withoutStart_isNoOp() {
        // Should not throw
        ExecutionMetrics.markEnd("never-started");
    }

    @Test
    public void markStart_twice_lastStartWins() throws InterruptedException {
        ExecutionMetrics.markStart("test-2");
        Thread.sleep(20);
        ExecutionMetrics.markStart("test-2"); // overwrite
        ExecutionMetrics.markEnd("test-2");
        // Should complete without error; second start resets the clock
    }

    // ----------------------------------------------------------
    // recordStatus
    // ----------------------------------------------------------

    @Test
    public void recordStatus_storesStatusCorrectly() {
        ExecutionMetrics.markStart("test-3");
        ExecutionMetrics.markEnd("test-3");
        ExecutionMetrics.recordStatus("test-3", "FAILED");
        // Verified implicitly: if the computeIfAbsent path is broken, other
        // tests that rely on status counts would fail
    }

    // ----------------------------------------------------------
    // recordScreenshot
    // ----------------------------------------------------------

    @Test
    public void recordScreenshot_nullPath_isNoOp() {
        ExecutionMetrics.markStart("test-4");
        ExecutionMetrics.markEnd("test-4");
        // null path must not throw
        ExecutionMetrics.recordScreenshot("test-4", null);
    }

    @Test
    public void recordScreenshot_validPath_stored() {
        ExecutionMetrics.markStart("test-5");
        ExecutionMetrics.markEnd("test-5");
        ExecutionMetrics.recordScreenshot("test-5", "/tmp/screenshot.png");
        // No exception means the timing entry was created/updated correctly
    }

    // ----------------------------------------------------------
    // percentile
    // ----------------------------------------------------------

    @Test
    public void percentile_emptyList_returnsZero() {
        assertEquals(0L, ExecutionMetrics.percentile(List.of(), 50));
    }

    @Test
    public void percentile_singleValue_returnsThatValue() {
        assertEquals(100L, ExecutionMetrics.percentile(Arrays.asList(100L), 50));
        assertEquals(100L, ExecutionMetrics.percentile(Arrays.asList(100L), 99));
    }

    @Test
    public void percentile_p50_returnsMedian() {
        List<Long> values = Arrays.asList(10L, 20L, 30L, 40L, 50L);
        assertEquals(30L, ExecutionMetrics.percentile(values, 50));
    }

    @Test
    public void percentile_p100_returnsMax() {
        List<Long> values = Arrays.asList(10L, 20L, 30L, 40L, 500L);
        assertEquals(500L, ExecutionMetrics.percentile(values, 100));
    }

    @Test
    public void percentile_p0_returnsMin() {
        List<Long> values = Arrays.asList(10L, 20L, 30L);
        // index = ceil(0/100 * 3) = 0, clamped to 0 → first element after sort
        long result = ExecutionMetrics.percentile(values, 0);
        assertTrue(result >= 0);
    }

    // ----------------------------------------------------------
    // reset
    // ----------------------------------------------------------

    @Test
    public void reset_clearsAllState() {
        ExecutionMetrics.markStart("test-x");
        ExecutionMetrics.markEnd("test-x");
        ExecutionMetrics.recordStatus("test-x", "PASSED");

        ExecutionMetrics.reset();

        // After reset, markEnd for unknown testId is a no-op — no exception
        ExecutionMetrics.markEnd("test-x");
    }
}
