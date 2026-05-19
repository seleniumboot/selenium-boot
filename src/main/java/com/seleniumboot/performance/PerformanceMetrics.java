package com.seleniumboot.performance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seleniumboot.api.SeleniumBootApi;

/**
 * Core Web Vitals and Navigation Timing metrics collected from the active browser page.
 *
 * <p>All time values are in <strong>milliseconds</strong> relative to the start of the
 * page navigation. A value of {@code -1} means the metric was not available in the current
 * browser (LCP and CLS are Chrome/Edge only; FCP requires Paint Timing API support).
 *
 * <h3>Google Web Vitals thresholds</h3>
 * <table>
 *   <tr><th>Metric</th><th>Good</th><th>Needs Improvement</th><th>Poor</th></tr>
 *   <tr><td>LCP</td><td>&lt; 2500 ms</td><td>&lt; 4000 ms</td><td>&ge; 4000 ms</td></tr>
 *   <tr><td>FCP</td><td>&lt; 1800 ms</td><td>&lt; 3000 ms</td><td>&ge; 3000 ms</td></tr>
 *   <tr><td>CLS</td><td>&lt; 0.1</td><td>&lt; 0.25</td><td>&ge; 0.25</td></tr>
 *   <tr><td>TTFB</td><td>&lt; 800 ms</td><td>&lt; 1800 ms</td><td>&ge; 1800 ms</td></tr>
 * </table>
 */
@SeleniumBootApi(since = "2.4.0")
public final class PerformanceMetrics {

    public static final double NOT_AVAILABLE = -1.0;

    @JsonProperty private final double lcp;       // ms — Largest Contentful Paint (Chrome/Edge only)
    @JsonProperty private final double fcp;       // ms — First Contentful Paint
    @JsonProperty private final double fp;        // ms — First Paint
    @JsonProperty private final double ttfb;      // ms — Time To First Byte (responseStart)
    @JsonProperty private final double cls;       // score — Cumulative Layout Shift (Chrome/Edge only)
    @JsonProperty private final double domLoad;   // ms — DOMContentLoaded
    @JsonProperty private final double pageLoad;  // ms — window.load event

    public PerformanceMetrics(double lcp, double fcp, double fp,
                       double ttfb, double cls, double domLoad, double pageLoad) {
        this.lcp      = lcp;
        this.fcp      = fcp;
        this.fp       = fp;
        this.ttfb     = ttfb;
        this.cls      = cls;
        this.domLoad  = domLoad;
        this.pageLoad = pageLoad;
    }

    /** Largest Contentful Paint in ms. {@code -1} if not available (non-Chrome browsers). */
    public double lcp()      { return lcp; }

    /** First Contentful Paint in ms. {@code -1} if not available. */
    public double fcp()      { return fcp; }

    /** First Paint in ms. {@code -1} if not available. */
    public double fp()       { return fp; }

    /** Time To First Byte in ms (server response time). {@code -1} if not available. */
    public double ttfb()     { return ttfb; }

    /** Cumulative Layout Shift score (0–∞, lower is better). {@code -1} if not available. */
    public double cls()      { return cls; }

    /** DOMContentLoaded event time in ms. {@code -1} if not available. */
    public double domLoad()  { return domLoad; }

    /** window.load event time in ms (full page including resources). {@code -1} if not available. */
    public double pageLoad() { return pageLoad; }

    /** Returns {@code true} if the metric value is available (not {@code -1}). */
    public boolean isAvailable(double value) { return value >= 0; }

    @Override
    public String toString() {
        return String.format(
            "PerformanceMetrics{lcp=%.0fms, fcp=%.0fms, ttfb=%.0fms, cls=%.3f, domLoad=%.0fms, pageLoad=%.0fms}",
            lcp, fcp, ttfb, cls, domLoad, pageLoad
        );
    }
}
