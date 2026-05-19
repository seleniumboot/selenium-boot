package com.seleniumboot.performance;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Map;

/**
 * Collects Core Web Vitals and Navigation Timing metrics from the active browser page
 * by executing the {@code window.performance} API via JavaScript.
 *
 * <p>No external dependencies — uses browser-native APIs only:
 * <ul>
 *   <li><b>Navigation Timing Level 2</b> — TTFB, DOMContentLoaded, page load</li>
 *   <li><b>Paint Timing</b> — First Paint, First Contentful Paint</li>
 *   <li><b>Largest Contentful Paint</b> — Chrome/Edge only (buffered entries)</li>
 *   <li><b>Layout Instability</b> — CLS — Chrome/Edge only (buffered entries)</li>
 * </ul>
 *
 * <p>Call {@link #collect()} after {@code open()} once the page has fully loaded.
 * Metrics requiring PerformanceObserver (LCP, CLS) are read from the browser's
 * buffered entry queue — no pre-injection needed.
 */
public final class PerformanceCollector {

    /**
     * JavaScript that reads all available performance entries and returns a plain object.
     * Runs entirely in the page context; safe for SPA and multi-page navigation.
     */
    private static final String COLLECT_JS =
        "(function() {" +
        "  var m = { lcp:-1, fcp:-1, fp:-1, ttfb:-1, cls:-1, domLoad:-1, pageLoad:-1 };" +

        // Navigation Timing Level 2 (all modern browsers)
        "  var nav = performance.getEntriesByType('navigation');" +
        "  if (nav && nav.length > 0) {" +
        "    var n = nav[0];" +
        "    m.ttfb     = n.responseStart > 0 ? n.responseStart : -1;" +
        "    m.domLoad  = n.domContentLoadedEventEnd > 0 ? n.domContentLoadedEventEnd : -1;" +
        "    m.pageLoad = n.loadEventEnd > 0 ? n.loadEventEnd : -1;" +
        "  }" +

        // Paint Timing (Chrome, Firefox, Edge — not Safari < 14.5)
        "  var paints = performance.getEntriesByType('paint');" +
        "  for (var i = 0; i < paints.length; i++) {" +
        "    if (paints[i].name === 'first-paint') m.fp = paints[i].startTime;" +
        "    if (paints[i].name === 'first-contentful-paint') m.fcp = paints[i].startTime;" +
        "  }" +

        // Largest Contentful Paint — buffered entries (Chrome/Edge only)
        "  var lcpList = performance.getEntriesByType('largest-contentful-paint');" +
        "  if (lcpList && lcpList.length > 0) {" +
        "    m.lcp = lcpList[lcpList.length - 1].startTime;" +
        "  }" +

        // Cumulative Layout Shift — buffered entries (Chrome/Edge only)
        "  var shiftList = performance.getEntriesByType('layout-shift');" +
        "  if (shiftList && shiftList.length > 0) {" +
        "    var cls = 0;" +
        "    for (var j = 0; j < shiftList.length; j++) {" +
        "      if (!shiftList[j].hadRecentInput) cls += shiftList[j].value;" +
        "    }" +
        "    m.cls = cls;" +
        "  }" +

        "  return m;" +
        "})();";

    private PerformanceCollector() {}

    /**
     * Collects Core Web Vitals from the currently loaded page.
     *
     * <p>Call this after {@code open()} and after any SPA transitions have settled.
     * For single-page applications, navigate to the target route first, then collect.
     *
     * @throws IllegalStateException if no active WebDriver session exists
     * @throws UnsupportedOperationException if the browser does not support JavaScript execution
     */
    public static PerformanceMetrics collect() {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException(
                "[Performance] No active WebDriver. Call collect() after open()."
            );
        }
        if (!(driver instanceof JavascriptExecutor)) {
            throw new UnsupportedOperationException(
                "[Performance] Browser does not support JavaScript execution."
            );
        }

        Object result = ((JavascriptExecutor) driver).executeScript(COLLECT_JS);
        return parse(result);
    }

    @SuppressWarnings("unchecked")
    private static PerformanceMetrics parse(Object raw) {
        if (!(raw instanceof Map)) {
            return unavailable();
        }
        Map<String, Object> m = (Map<String, Object>) raw;
        return new PerformanceMetrics(
            toDouble(m.get("lcp")),
            toDouble(m.get("fcp")),
            toDouble(m.get("fp")),
            toDouble(m.get("ttfb")),
            toDouble(m.get("cls")),
            toDouble(m.get("domLoad")),
            toDouble(m.get("pageLoad"))
        );
    }

    private static double toDouble(Object value) {
        if (value == null) return PerformanceMetrics.NOT_AVAILABLE;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return PerformanceMetrics.NOT_AVAILABLE; }
    }

    private static PerformanceMetrics unavailable() {
        double na = PerformanceMetrics.NOT_AVAILABLE;
        return new PerformanceMetrics(na, na, na, na, na, na, na);
    }
}
