---
id: retry
title: Retry
sidebar_position: 4
---

# Retry

Selenium Boot retries failed tests automatically. No `IRetryAnalyzer` wiring required.

---

## Global retry

Enable retry for all tests in `selenium-boot.yml`:

```yaml
retry:
  enabled: true
  maxAttempts: 2   # total attempts (including the first run)
```

`maxAttempts: 2` means: run once, if it fails, retry once more.

---

## Per-method override

Use `@Retryable` to override the global setting for a specific test:

```java
import com.seleniumboot.listeners.Retryable;

@Test
@Retryable(maxAttempts = 3)
public void flakyTest() {
    // retried up to 3 times regardless of global config
}
```

---

## Disabling retry globally

```yaml
retry:
  enabled: false
```

When disabled, `@Retryable` on individual methods is also ignored.

---

## In the HTML report

The **Dashboard tab** shows a Retry Summary card when any test was retried:

| Metric | Meaning |
|---|---|
| **Retried** | Tests that failed at least once but were retried |
| **Recovered** | Tests that eventually passed after retry |
| **Still Failing** | Tests that failed all attempts |

Retried tests are marked with a `↻ Nx` badge in the Test Cases table.

---

## How it works

`RetryAnnotationTransformer` is registered via Java SPI (`META-INF/services/org.testng.ITestNGListener`). It is discovered automatically when `selenium-boot` is on the classpath — no listener registration needed.

At runtime, `RetryListener` checks the global config and `@Retryable` annotation, then returns `true` (retry) or `false` (stop) to TestNG.
