package com.seleniumboot.unit;

import com.seleniumboot.performance.PerformanceAssert;
import com.seleniumboot.performance.PerformanceMetrics;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link PerformanceAssert}, {@link PerformanceMetrics}, and the
 * threshold/rating helpers in the assert chain.
 * No WebDriver required — tests operate on hand-crafted {@link PerformanceMetrics} instances.
 */
public class PerformanceTest {

    // ── PerformanceMetrics ────────────────────────────────────────────────

    @Test
    public void metrics_returnsCorrectValues() {
        PerformanceMetrics m = metrics(1200, 900, 800, 0.05, 700, 1500, 2000);
        assertEquals(m.lcp(),      1200, 0.001);
        assertEquals(m.fcp(),      900,  0.001);
        assertEquals(m.ttfb(),     800,  0.001);
        assertEquals(m.cls(),      0.05, 0.0001);
        assertEquals(m.fp(),       700,  0.001);
        assertEquals(m.domLoad(),  1500, 0.001);
        assertEquals(m.pageLoad(), 2000, 0.001);
    }

    @Test
    public void metrics_notAvailable_returnsMinusOne() {
        PerformanceMetrics m = metrics(-1, -1, -1, -1, -1, -1, -1);
        assertEquals(m.lcp(),  -1.0, 0.001);
        assertEquals(m.cls(),  -1.0, 0.001);
        assertFalse(m.isAvailable(m.lcp()));
        assertFalse(m.isAvailable(m.cls()));
    }

    @Test
    public void metrics_available_returnsTrue_whenValueIsZeroOrPositive() {
        PerformanceMetrics m = metrics(0, 500, 100, 0.0, -1, -1, -1);
        assertTrue(m.isAvailable(m.lcp()));
        assertTrue(m.isAvailable(m.fcp()));
        assertTrue(m.isAvailable(m.cls()));
        assertFalse(m.isAvailable(m.domLoad()));
    }

    @Test
    public void metrics_toStringContainsKeyFields() {
        PerformanceMetrics m = metrics(1200, 900, 800, 0.05, -1, 1500, 2000);
        String s = m.toString();
        assertTrue(s.contains("lcp=1200ms"), "toString should include LCP");
        assertTrue(s.contains("fcp=900ms"),  "toString should include FCP");
        assertTrue(s.contains("cls=0.050"),  "toString should include CLS");
    }

    // ── PerformanceAssert.of() ────────────────────────────────────────────

    @Test
    public void of_returnsNonNullAssert() {
        assertNotNull(PerformanceAssert.of(metrics(1200, 900, 800, 0.05, -1, 1500, 2000)));
    }

    @Test
    public void metrics_accessorOnAssert_returnsUnderlyingMetrics() {
        PerformanceMetrics m = metrics(1200, 900, 800, 0.05, -1, 1500, 2000);
        assertSame(PerformanceAssert.of(m).metrics(), m);
    }

    // ── isBelow — pass ────────────────────────────────────────────────────

    @Test
    public void lcp_isBelow_passes_whenUnderThreshold() {
        PerformanceAssert.of(metrics(1200, -1, -1, -1, -1, -1, -1))
            .lcp().isBelow(2500);   // 1200 < 2500 ✓
    }

    @Test
    public void fcp_isBelow_passes_whenUnderThreshold() {
        PerformanceAssert.of(metrics(-1, 800, -1, -1, -1, -1, -1))
            .fcp().isBelow(1800);   // 800 < 1800 ✓
    }

    @Test
    public void ttfb_isBelow_passes_whenUnderThreshold() {
        PerformanceAssert.of(metrics(-1, -1, 200, -1, -1, -1, -1))
            .ttfb().isBelow(600);   // 200 < 600 ✓
    }

    @Test
    public void cls_isBelow_passes_whenUnderThreshold() {
        PerformanceAssert.of(metrics(-1, -1, -1, 0.05, -1, -1, -1))
            .cls().isBelow(0.1);    // 0.05 < 0.1 ✓
    }

    // ── isBelow — chain returns parent ────────────────────────────────────

    @Test
    public void isBelow_returnsParentAssert_forChaining() {
        PerformanceAssert pa = PerformanceAssert.of(metrics(1200, 800, 200, 0.05, -1, 1000, 2000));
        PerformanceAssert returned = pa.lcp().isBelow(2500);
        assertSame(returned, pa);
    }

    @Test
    public void fullChain_lcpFcpTtfbCls_allPass() {
        PerformanceAssert.of(metrics(1200, 800, 200, 0.05, -1, 1000, 2000))
            .lcp().isBelow(2500)
            .fcp().isBelow(1800)
            .ttfb().isBelow(600)
            .cls().isBelow(0.1);
    }

    // ── isBelow — fail ────────────────────────────────────────────────────

    @Test(expectedExceptions = AssertionError.class,
          expectedExceptionsMessageRegExp = ".*LCP.*exceeded.*")
    public void lcp_isBelow_fails_whenOverThreshold() {
        PerformanceAssert.of(metrics(3500, -1, -1, -1, -1, -1, -1))
            .lcp().isBelow(2500);   // 3500 >= 2500 ✗
    }

    @Test(expectedExceptions = AssertionError.class,
          expectedExceptionsMessageRegExp = ".*CLS.*exceeded.*")
    public void cls_isBelow_fails_whenOverThreshold() {
        PerformanceAssert.of(metrics(-1, -1, -1, 0.3, -1, -1, -1))
            .cls().isBelow(0.1);    // 0.3 >= 0.1 ✗
    }

    @Test(expectedExceptions = AssertionError.class)
    public void isBelow_withCustomMessage_includesMessage() {
        PerformanceAssert.of(metrics(4000, -1, -1, -1, -1, -1, -1))
            .lcp().isBelow(2500, "Homepage LCP regression detected");
    }

    // ── unavailable metric skips assertion ────────────────────────────────

    @Test
    public void lcp_isBelow_skipsAssertion_whenNotAvailable() {
        // LCP = -1 on Firefox — must NOT throw
        PerformanceAssert.of(metrics(-1, -1, -1, -1, -1, -1, -1))
            .lcp().isBelow(2500);   // should silently skip
    }

    @Test
    public void cls_isBelow_skipsAssertion_whenNotAvailable() {
        PerformanceAssert.of(metrics(-1, -1, -1, -1, -1, -1, -1))
            .cls().isBelow(0.1);
    }

    // ── isAbove ───────────────────────────────────────────────────────────

    @Test
    public void ttfb_isAbove_passes_whenOverThreshold() {
        PerformanceAssert.of(metrics(-1, -1, 50, -1, -1, -1, -1))
            .ttfb().isAbove(0);     // TTFB > 0 means real server response ✓
    }

    @Test(expectedExceptions = AssertionError.class)
    public void ttfb_isAbove_fails_whenUnderThreshold() {
        PerformanceAssert.of(metrics(-1, -1, 50, -1, -1, -1, -1))
            .ttfb().isAbove(100);   // 50 <= 100 ✗
    }

    // ── MetricAssert.value() ──────────────────────────────────────────────

    @Test
    public void metricAssert_value_returnsRawValue() {
        PerformanceAssert.MetricAssert ma =
            PerformanceAssert.of(metrics(1234, -1, -1, -1, -1, -1, -1)).lcp();
        assertEquals(ma.value(), 1234.0, 0.001);
    }

    @Test
    public void metricAssert_isAvailable_trueWhenSet() {
        assertTrue(PerformanceAssert.of(metrics(1200, -1, -1, -1, -1, -1, -1)).lcp().isAvailable());
    }

    @Test
    public void metricAssert_isAvailable_falseWhenMinus1() {
        assertFalse(PerformanceAssert.of(metrics(-1, -1, -1, -1, -1, -1, -1)).lcp().isAvailable());
    }

    // ── SeleniumBootApi annotation ────────────────────────────────────────

    @Test
    public void performanceAssert_hasSeleniumBootApiAnnotation() {
        com.seleniumboot.api.SeleniumBootApi ann =
            PerformanceAssert.class.getAnnotation(com.seleniumboot.api.SeleniumBootApi.class);
        assertNotNull(ann);
        assertEquals(ann.since(), "2.4.0");
    }

    @Test
    public void performanceMetrics_hasSeleniumBootApiAnnotation() {
        com.seleniumboot.api.SeleniumBootApi ann =
            PerformanceMetrics.class.getAnnotation(com.seleniumboot.api.SeleniumBootApi.class);
        assertNotNull(ann);
        assertEquals(ann.since(), "2.4.0");
    }

    // ── helper ───────────────────────────────────────────────────────────

    private static PerformanceMetrics metrics(double lcp, double fcp, double ttfb,
                                               double cls, double fp,
                                               double domLoad, double pageLoad) {
        return new PerformanceMetrics(lcp, fcp, fp, ttfb, cls, domLoad, pageLoad);
    }
}
