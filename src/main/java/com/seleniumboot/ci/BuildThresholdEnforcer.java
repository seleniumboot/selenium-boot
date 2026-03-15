package com.seleniumboot.ci;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.metrics.TestTiming;

import java.util.Collection;

/**
 * Enforces build quality gates defined in the {@code ci:} config block.
 *
 * <p>Configured via {@code selenium-boot.yml}:
 * <pre>
 * ci:
 *   failOnPassRateBelow: 80   # fail build if pass rate drops below 80%
 *   maxFlakyTests: 3          # fail build if more than 3 tests were retried
 * </pre>
 *
 * <p>Throws {@link BuildQualityGateException} if any threshold is breached.
 * This propagates through {@code SuiteExecutionListener.onFinish()} and causes
 * Maven/Gradle to mark the build as failed.
 */
public final class BuildThresholdEnforcer {

    private BuildThresholdEnforcer() {}

    public static void enforce(SeleniumBootConfig config, Collection<TestTiming> timings) {
        SeleniumBootConfig.Ci ci = config.getCi();
        if (ci == null) {
            return; // no thresholds configured — nothing to enforce
        }

        int total   = timings.size();
        if (total == 0) {
            return;
        }

        long passed  = timings.stream().filter(t -> "PASSED".equals(t.getStatus())).count();
        long failed  = timings.stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        // --- Pass rate gate ---
        double threshold = ci.getFailOnPassRateBelow();
        if (threshold > 0) {
            double passRate = (passed * 100.0) / total;
            if (passRate < threshold) {
                throw new BuildQualityGateException(String.format(
                        "Build quality gate FAILED: pass rate %.1f%% is below the required %.1f%% "
                        + "(%d passed, %d failed, %d total).",
                        passRate, threshold, passed, failed, total));
            }
            System.out.printf("[Selenium Boot] Pass rate: %.1f%% (required ≥ %.1f%%) ✓%n",
                    passRate, threshold);
        }

        // --- Flaky test gate ---
        int maxFlaky = ci.getMaxFlakyTests();
        if (maxFlaky >= 0) {
            long flaky = timings.stream()
                    .filter(t -> t.getRetryCount() > 0 && "PASSED".equals(t.getStatus()))
                    .count();
            if (flaky > maxFlaky) {
                throw new BuildQualityGateException(String.format(
                        "Build quality gate FAILED: %d flaky tests detected (max allowed: %d).",
                        flaky, maxFlaky));
            }
            System.out.printf("[Selenium Boot] Flaky tests: %d (max allowed: %d) ✓%n",
                    flaky, maxFlaky);
        }
    }
}
