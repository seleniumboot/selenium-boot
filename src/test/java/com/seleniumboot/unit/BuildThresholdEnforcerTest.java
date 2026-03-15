package com.seleniumboot.unit;

import com.seleniumboot.ci.BuildQualityGateException;
import com.seleniumboot.ci.BuildThresholdEnforcer;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.metrics.TestTiming;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link BuildThresholdEnforcer}.
 * No browser, no framework bootstrap required.
 */
public class BuildThresholdEnforcerTest {

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private SeleniumBootConfig configWithThresholds(double passRate, int maxFlaky) {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Ci ci = new SeleniumBootConfig.Ci();
        ci.setFailOnPassRateBelow(passRate);
        ci.setMaxFlakyTests(maxFlaky);
        config.setCi(ci);
        return config;
    }

    private TestTiming timing(String id, String status, int retries) {
        TestTiming t = new TestTiming(id, "main");
        t.setStatus(status);
        for (int i = 0; i < retries; i++) {
            t.incrementRetryCount();
        }
        return t;
    }

    // ----------------------------------------------------------
    // No ci: block
    // ----------------------------------------------------------

    @Test
    public void noCiConfig_noExceptionThrown() {
        SeleniumBootConfig config = new SeleniumBootConfig();
        // ci is null — enforcer must be a no-op
        BuildThresholdEnforcer.enforce(config, List.of(timing("t1", "FAILED", 0)));
    }

    // ----------------------------------------------------------
    // Empty timings
    // ----------------------------------------------------------

    @Test
    public void emptyTimings_noExceptionThrown() {
        SeleniumBootConfig config = configWithThresholds(80, 0);
        BuildThresholdEnforcer.enforce(config, new ArrayList<>());
    }

    // ----------------------------------------------------------
    // Pass rate gate — PASS scenarios
    // ----------------------------------------------------------

    @Test
    public void passRate_exactlyAtThreshold_passes() {
        SeleniumBootConfig config = configWithThresholds(80, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 0),
                timing("t2", "PASSED", 0),
                timing("t3", "PASSED", 0),
                timing("t4", "PASSED", 0),
                timing("t5", "FAILED", 0)   // 80% pass rate
        );
        BuildThresholdEnforcer.enforce(config, timings); // must not throw
    }

    @Test
    public void passRate_aboveThreshold_passes() {
        SeleniumBootConfig config = configWithThresholds(50, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 0),
                timing("t2", "PASSED", 0),
                timing("t3", "FAILED", 0)   // 66% > 50%
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Pass rate gate — FAIL scenarios
    // ----------------------------------------------------------

    @Test(expectedExceptions = BuildQualityGateException.class)
    public void passRate_belowThreshold_throwsException() {
        SeleniumBootConfig config = configWithThresholds(80, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 0),
                timing("t2", "FAILED", 0),
                timing("t3", "FAILED", 0)   // 33% < 80%
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    @Test(expectedExceptions = BuildQualityGateException.class)
    public void passRate_allFailed_throwsException() {
        SeleniumBootConfig config = configWithThresholds(1, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "FAILED", 0),
                timing("t2", "FAILED", 0)
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Pass rate disabled (0)
    // ----------------------------------------------------------

    @Test
    public void passRate_zeroThreshold_disabled_noException() {
        SeleniumBootConfig config = configWithThresholds(0, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "FAILED", 0),
                timing("t2", "FAILED", 0)   // 0% pass — but gate is disabled
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Flaky test gate — PASS scenarios
    // ----------------------------------------------------------

    @Test
    public void flakyGate_withinLimit_passes() {
        SeleniumBootConfig config = configWithThresholds(0, 2);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 1),  // flaky (retried, then passed)
                timing("t2", "PASSED", 0),
                timing("t3", "PASSED", 0)
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    @Test
    public void flakyGate_exactlyAtLimit_passes() {
        SeleniumBootConfig config = configWithThresholds(0, 2);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 1),
                timing("t2", "PASSED", 1),
                timing("t3", "PASSED", 0)
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Flaky test gate — FAIL scenarios
    // ----------------------------------------------------------

    @Test(expectedExceptions = BuildQualityGateException.class)
    public void flakyGate_exceedsLimit_throwsException() {
        SeleniumBootConfig config = configWithThresholds(0, 1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 2),  // flaky
                timing("t2", "PASSED", 1),  // flaky — 2 flaky > max 1
                timing("t3", "PASSED", 0)
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Flaky gate disabled (-1)
    // ----------------------------------------------------------

    @Test
    public void flakyGate_minusOne_disabled_noException() {
        SeleniumBootConfig config = configWithThresholds(0, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 5),
                timing("t2", "PASSED", 5)
        );
        BuildThresholdEnforcer.enforce(config, timings);
    }

    // ----------------------------------------------------------
    // Exception message quality
    // ----------------------------------------------------------

    @Test
    public void passRateException_containsActualAndRequiredRate() {
        SeleniumBootConfig config = configWithThresholds(90, -1);
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED", 0),
                timing("t2", "FAILED", 0)   // 50% < 90%
        );
        try {
            BuildThresholdEnforcer.enforce(config, timings);
            fail("Expected BuildQualityGateException");
        } catch (BuildQualityGateException e) {
            assertTrue(e.getMessage().contains("90"),
                    "Exception message should reference the threshold value");
        }
    }
}
