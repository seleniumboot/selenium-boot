---
id: performance
title: Performance Assertions
sidebar_position: 16
---

# Performance Assertions (Core Web Vitals)

Selenium Boot 2.4.0 lets you assert Google's Core Web Vitals directly in your Selenium tests — no extra tool, no proxy, no external service. Call `assertPerformance()` after `open()` and set thresholds for each metric.

---

## Quick example

```java
public class HomepagePerformanceTest extends BaseTest {

    @Test
    public void homepage_meetsWebVitalThresholds() {
        open("/");

        assertPerformance()
            .lcp().isBelow(2500)     // Largest Contentful Paint  < 2.5 s  (Good)
            .fcp().isBelow(1800)     // First Contentful Paint    < 1.8 s  (Good)
            .ttfb().isBelow(600)     // Time To First Byte        < 600 ms (Good)
            .cls().isBelow(0.1);     // Cumulative Layout Shift   < 0.1    (Good)
    }
}
```

---

## Google Web Vitals thresholds

| Metric | Good | Needs Improvement | Poor |
|--------|------|-------------------|------|
| **LCP** — Largest Contentful Paint | < 2500 ms | < 4000 ms | ≥ 4000 ms |
| **FCP** — First Contentful Paint | < 1800 ms | < 3000 ms | ≥ 3000 ms |
| **CLS** — Cumulative Layout Shift | < 0.1 | < 0.25 | ≥ 0.25 |
| **TTFB** — Time To First Byte | < 800 ms | < 1800 ms | ≥ 1800 ms |

---

## API reference

### `assertPerformance()`

Available in `BaseTest` and `BaseJUnit5Test`. Collects metrics from the current page and returns a fluent `PerformanceAssert` builder.

```java
assertPerformance()
    .lcp().isBelow(2500)
    .fcp().isBelow(1800)
    .ttfb().isBelow(600)
    .cls().isBelow(0.1)
    .domLoad().isBelow(3000)    // DOMContentLoaded
    .pageLoad().isBelow(5000);  // window.load (full page)
```

### `isBelow(double threshold)`

Asserts the metric is strictly below the threshold. Throws `AssertionError` on failure with a message like:

```
[Performance] LCP exceeded threshold: LCP = 3420ms (threshold: < 2500ms)
```

### `isBelow(double threshold, String message)`

Same as above with a custom failure message:

```java
assertPerformance()
    .lcp().isBelow(2500, "Homepage LCP regression after hero image update");
```

### `isAbove(double threshold)`

For asserting a lower bound — useful to verify real server latency is measurable:

```java
assertPerformance()
    .ttfb().isAbove(0);   // confirms a real HTTP round-trip occurred
```

### `collectPerformance()`

Returns raw `PerformanceMetrics` for custom assertions or logging:

```java
PerformanceMetrics perf = collectPerformance();

System.out.println("LCP: " + perf.lcp() + "ms");
System.out.println("CLS: " + perf.cls());

Assert.assertTrue(perf.lcp() < 3000, "LCP regression: " + perf.lcp() + "ms");
```

### Chaining

Each `isBelow()` / `isAbove()` call returns the parent `PerformanceAssert`, enabling full chaining:

```java
assertPerformance()
    .lcp().isBelow(2500)
    .fcp().isBelow(1800)
    .ttfb().isBelow(600)
    .cls().isBelow(0.1);
```

---

## Metric availability by browser

| Metric | Chrome | Edge | Firefox | Safari |
|--------|--------|------|---------|--------|
| LCP | ✅ | ✅ | ❌ | ❌ |
| FCP | ✅ | ✅ | ✅ | ✅ (14.5+) |
| TTFB | ✅ | ✅ | ✅ | ✅ |
| CLS | ✅ | ✅ | ❌ | ❌ |
| DOMContentLoaded | ✅ | ✅ | ✅ | ✅ |
| Page Load | ✅ | ✅ | ✅ | ✅ |

When a metric is unavailable in the current browser (value = `-1`), the assertion is **silently skipped** — it does not fail. This lets you write cross-browser test suites that assert every metric that can be measured in each browser.

---

## Auto-capture in HTML report

Enable automatic collection after every passing test:

```yaml title="selenium-boot.yml"
performance:
  captureOnEveryTest: true
```

When enabled, the test detail panel in the HTML report shows a **⚡ Performance** strip with colour-coded chips for each available metric:

| Colour | Meaning |
|--------|---------|
| 🟢 Green | Good threshold met |
| 🟡 Yellow | Needs improvement |
| 🔴 Red | Poor — exceeds threshold |
| Grey (italic) | DOM load / Page load (informational only) |

---

## When to call it

`assertPerformance()` reads the browser's performance timeline **at the moment it's called**. For best results:

- Call it **after** the page has fully loaded — right after `open()` is fine for traditional MPA pages
- For **SPAs**, wait for the route to settle before collecting: use `waitForAngular()`, `waitForReactHydration()`, or an explicit element wait first
- LCP is finalized when the user stops interacting — in an automated test, call it before any clicks to get the accurate value

---

## Combining with other assertions

Performance assertions are just regular TestNG / JUnit 5 assertions — combine them freely:

```java
@Test
public void productPage_loadsQuicklyAndRendersCorrectly() {
    open("/products/123");

    // Functional assertion
    assertThat(By.cssSelector(".product-title")).isVisible();

    // Performance assertion
    assertPerformance()
        .lcp().isBelow(2500)
        .cls().isBelow(0.1);

    // Business assertion
    assertThat(By.cssSelector(".add-to-cart")).isEnabled();
}
```

---

## Config reference

```yaml title="selenium-boot.yml"
performance:
  captureOnEveryTest: false   # auto-capture and show in HTML report (default off)
  lcpWarnMs:  2500            # (future) report warning when LCP exceeds this; 0 = disabled
  fcpWarnMs:  1800
  ttfbWarnMs: 800
  clsWarn:    0.1
```
