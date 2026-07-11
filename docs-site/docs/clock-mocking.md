---
description: "Mock the browser clock in Selenium tests: freeze or advance JavaScript Date to test countdowns, trials, and time-sensitive UI deterministically."
id: clock-mocking
title: Clock Mocking
sidebar_position: 14
---

# Clock Mocking

`TestClock` lets you freeze or advance the browser's `Date` object so you can test time-sensitive UI without touching the database or the system clock.

---

## How it works

`clock().set(isoString)` injects a `Date` override into the active browser page. Every subsequent `new Date()` and `Date.now()` call in client-side JavaScript returns the mocked time. The real `Date` is saved under `window.__sbOriginalDate` and restored automatically at the end of each test.

---

## Quick example

```java
public class TrialBannerTest extends BaseTest {

    @Test
    public void showsExpiredBanner_when30DaysPast() {
        open("/dashboard");
        clock().set("2030-06-01T00:00:00Z");    // trial expired 30 days ago
        getDriver().navigate().refresh();        // page re-renders with mocked time

        assertThat(By.id("trial-banner")).hasText("Your trial expired 30 days ago");
    }
}
```

:::tip When to call `open()` first
`clock().set()` requires an active page because it injects JavaScript. Call `open()` first, then set the clock, then trigger any client-side re-render (refresh, SPA navigation, or a click that fetches dates).
:::

---

## API reference

### `clock().set(String isoDateTime)`

Overrides `new Date()` and `Date.now()` in the browser to the given instant.

```java
clock().set("2030-01-01T00:00:00Z");
```

- Accepts any ISO 8601 UTC string (`Instant.parse` compatible)
- Returns `this` for chaining

### `clock().advance(Duration duration)`

Advances the mocked time by `duration` from the current mock. If no mock is active, advances from the real current time.

```java
clock().set("2030-01-01T00:00:00Z");
clock().advance(Duration.ofDays(30));   // now mocked to 2030-01-31
```

Returns `this` for chaining. Common durations:

```java
Duration.ofSeconds(30)
Duration.ofMinutes(5)
Duration.ofHours(1)
Duration.ofDays(90)
```

### `clock().reset()`

Restores the real `Date` implementation in the browser. Called automatically after each test — explicit calls are optional.

```java
clock().set("2030-01-01T00:00:00Z");
// ... assertions ...
clock().reset();  // optional — framework does this automatically
```

### `clock().getMockedTimeMs()`

Returns the currently mocked time as epoch milliseconds, or `null` if no mock is active.

---

## Common use cases

### Trial / subscription expiry

```java
// Move 30 days past the trial end date
clock().set("2030-06-01T00:00:00Z");
getDriver().navigate().refresh();
assertThat(By.id("trial-expired-banner")).isVisible();
```

### Upcoming promotion not yet active

```java
// One day before promotion starts — button should be hidden
clock().set("2030-04-30T23:59:59Z");
getDriver().navigate().refresh();
assertThat(By.id("promo-btn")).isHidden();

// Day of promotion — button should appear
clock().advance(Duration.ofSeconds(2));
getDriver().navigate().refresh();
assertThat(By.id("promo-btn")).isVisible();
```

### Countdown timers

```java
open("/sale");
clock().set("2030-07-04T11:00:00Z");
getDriver().navigate().refresh();
assertThat(By.id("countdown")).containsText("1 hour remaining");
```

### Scheduled job indicator

```java
clock().set("2030-12-31T23:59:00Z");
getDriver().navigate().refresh();
assertThat(By.id("year-end-notice")).isVisible();
```

---

## Chaining

```java
open("/account");
clock()
    .set("2030-01-01T00:00:00Z")
    .advance(Duration.ofDays(365));   // now at 2031-01-01
getDriver().navigate().refresh();
assertThat(By.id("anniversary-badge")).isVisible();
```

---

## Auto-reset

The framework calls `TestClock.autoReset()` after every test in all outcome paths (pass, fail, skip). Each test starts with a clean, real `Date`. You never need to call `clock().reset()` explicitly.

---

## Scope

`TestClock` controls the `Date` object **only within the currently loaded page** in the browser. It does not affect:

- Server-side date/time checks (use a server-side date override or environment variable for those)
- Other browser tabs / windows
- Java code running in the test JVM

---

## Config

```yaml title="selenium-boot.yml"
clock:
  injectHeader: false      # send X-Mock-Date header to server with each request
  headerName: X-Mock-Date  # header name (requires browser CDP support)
```

`injectHeader` is off by default. When enabled, every browser request will include the `X-Mock-Date` header set to the current mock time, allowing backend services that respect this header to simulate the same date server-side.
