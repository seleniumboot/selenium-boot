package com.seleniumboot.performance;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * Fluent assertion API for Core Web Vitals and Navigation Timing metrics.
 *
 * <p>Create an instance via {@link #of(PerformanceMetrics)} (or through
 * {@code assertPerformance()} in {@code BaseTest}) then chain metric assertions:
 *
 * <pre>
 * open("/dashboard");
 *
 * assertPerformance()
 *     .lcp().isBelow(2500)      // Largest Contentful Paint &lt; 2.5 s (Good threshold)
 *     .fcp().isBelow(1800)      // First Contentful Paint &lt; 1.8 s
 *     .ttfb().isBelow(600)      // Time To First Byte &lt; 600 ms
 *     .cls().isBelow(0.1);      // Cumulative Layout Shift &lt; 0.1 (Good threshold)
 * </pre>
 *
 * <p>If a metric is unavailable in the current browser (e.g., LCP on Firefox),
 * the assertion is skipped with a console warning rather than failing the test.
 * This allows writing cross-browser tests that assert only what's measurable.
 *
 * <p>Access raw values for custom assertions:
 * <pre>
 * PerformanceMetrics perf = collectPerformance();
 * Assert.assertTrue(perf.lcp() &lt; 3000, "LCP regression detected");
 * </pre>
 */
@SeleniumBootApi(since = "2.4.0")
public final class PerformanceAssert {

    private final PerformanceMetrics metrics;

    private PerformanceAssert(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Creates a {@code PerformanceAssert} wrapping the given metrics snapshot.
     * Prefer {@code assertPerformance()} in {@code BaseTest} over calling this directly.
     */
    public static PerformanceAssert of(PerformanceMetrics metrics) {
        return new PerformanceAssert(metrics);
    }

    /** Begins an assertion on the Largest Contentful Paint (ms). Chrome/Edge only. */
    public MetricAssert lcp() {
        return new MetricAssert("LCP", "ms", metrics.lcp(), this);
    }

    /** Begins an assertion on the First Contentful Paint (ms). */
    public MetricAssert fcp() {
        return new MetricAssert("FCP", "ms", metrics.fcp(), this);
    }

    /** Begins an assertion on the First Paint (ms). */
    public MetricAssert fp() {
        return new MetricAssert("FP", "ms", metrics.fp(), this);
    }

    /** Begins an assertion on the Time To First Byte (ms). */
    public MetricAssert ttfb() {
        return new MetricAssert("TTFB", "ms", metrics.ttfb(), this);
    }

    /**
     * Begins an assertion on the Cumulative Layout Shift score (unitless, lower is better).
     * Chrome/Edge only.
     */
    public MetricAssert cls() {
        return new MetricAssert("CLS", "", metrics.cls(), this);
    }

    /** Begins an assertion on the DOMContentLoaded time (ms). */
    public MetricAssert domLoad() {
        return new MetricAssert("DOMContentLoaded", "ms", metrics.domLoad(), this);
    }

    /** Begins an assertion on the full page load time (ms). */
    public MetricAssert pageLoad() {
        return new MetricAssert("PageLoad", "ms", metrics.pageLoad(), this);
    }

    /** Returns the underlying {@link PerformanceMetrics} for raw value access. */
    public PerformanceMetrics metrics() {
        return metrics;
    }

    // ── MetricAssert ──────────────────────────────────────────────────────────

    /**
     * Single-metric assertion that returns the parent {@link PerformanceAssert} on success,
     * enabling fluent chaining: {@code .lcp().isBelow(2500).fcp().isBelow(1800)}.
     */
    public static final class MetricAssert {

        private final String name;
        private final String unit;
        private final double value;
        private final PerformanceAssert parent;

        MetricAssert(String name, String unit, double value, PerformanceAssert parent) {
            this.name   = name;
            this.unit   = unit;
            this.value  = value;
            this.parent = parent;
        }

        /**
         * Asserts that the metric is strictly below {@code threshold}.
         * If the metric is unavailable, logs a warning and skips the assertion.
         *
         * @return the parent {@link PerformanceAssert} for continued chaining
         * @throws AssertionError if the metric value is &ge; {@code threshold}
         */
        public PerformanceAssert isBelow(double threshold) {
            return isBelow(threshold, null);
        }

        /**
         * Asserts that the metric is strictly below {@code threshold}, using {@code message}
         * as the failure description.
         */
        public PerformanceAssert isBelow(double threshold, String message) {
            if (value < 0) {
                System.out.println("[Performance] " + name + " is not available on this browser — assertion skipped.");
                return parent;
            }
            if (value >= threshold) {
                String formatted = name.equals("CLS")
                    ? String.format("%.4f", value)
                    : String.format("%.0f%s", value, unit);
                String thresholdStr = name.equals("CLS")
                    ? String.valueOf(threshold)
                    : threshold + unit;
                String detail = message != null ? message : name + " exceeded threshold";
                throw new AssertionError(
                    "[Performance] " + detail + ": " + name + " = " + formatted +
                    " (threshold: < " + thresholdStr + ")"
                );
            }
            return parent;
        }

        /**
         * Asserts that the metric is strictly above {@code threshold}.
         * Useful for asserting a baseline minimum (e.g., TTFB &gt; 0 on a real server).
         */
        public PerformanceAssert isAbove(double threshold) {
            if (value < 0) {
                System.out.println("[Performance] " + name + " is not available on this browser — assertion skipped.");
                return parent;
            }
            if (value <= threshold) {
                throw new AssertionError(
                    "[Performance] " + name + " = " + value + unit +
                    " is not above threshold " + threshold + unit
                );
            }
            return parent;
        }

        /**
         * Returns the raw metric value (ms or score). Useful for custom assertions or logging.
         * Returns {@code -1} if the metric is not available.
         */
        public double value() { return value; }

        /**
         * Returns {@code true} if the metric value is available in the current browser.
         */
        public boolean isAvailable() { return value >= 0; }
    }
}
