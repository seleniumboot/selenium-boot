---
id: configuration
title: Configuration Reference
sidebar_position: 3
---

# Configuration Reference

All framework behaviour is controlled by `selenium-boot.yml`.

---

## File resolution order

The framework looks for the config file in this priority order:

1. **System property** — `-Dselenium.boot.config=/path/to/custom.yml`
2. **Working directory** — `./selenium-boot.yml` (next to `pom.xml`)
3. **Classpath** — `src/test/resources/selenium-boot.yml`

---

## Full reference

```yaml
# ── Browser ────────────────────────────────────────────────────────────────
browser:
  name: chrome              # chrome | firefox | edge | safari
  headless: false           # true in CI (auto-forced when CI detected)
  lifecycle: per-test       # per-test (default) | per-suite

  # Optional: extra browser arguments
  arguments:
    - --start-maximized
    - --disable-notifications

  # Optional: raw capability overrides
  capabilities:
    acceptInsecureCerts: true

# ── Execution ───────────────────────────────────────────────────────────────
execution:
  mode: local               # local | remote
  baseUrl: https://your-app.com
  gridUrl: http://localhost:4444   # only for remote mode

  parallel: none            # none | methods | classes | tests
  threadCount: 1            # ignored when parallel: none
  maxActiveSessions: 5      # max concurrent browser instances (semaphore)

# ── Retry ───────────────────────────────────────────────────────────────────
retry:
  enabled: true             # applies to all tests globally
  maxAttempts: 2            # total attempts (1 = no retry)

# ── Timeouts ────────────────────────────────────────────────────────────────
timeouts:
  explicit: 10              # seconds — WaitEngine default timeout
  pageLoad: 30              # seconds — browser page load timeout

# ── CI / Build Quality Gates ────────────────────────────────────────────────
ci:
  failOnPassRateBelow: 80   # 0 = disabled. Fail build if pass rate < 80%
  maxFlakyTests: 3          # -1 = disabled. Fail if more than 3 tests retried
```

---

## Browser

### `name`
The browser to use. Selenium Manager downloads the matching driver automatically.

| Value | Browser |
|---|---|
| `chrome` | Google Chrome (default) |
| `firefox` | Mozilla Firefox |
| `edge` | Microsoft Edge |
| `safari` | Safari (macOS only) |

### `headless`
Runs the browser without a visible window. Automatically forced to `true` when a CI environment is detected (GitHub Actions, Jenkins, etc.).

### `lifecycle`
Controls when the WebDriver session is closed.

| Value | Behaviour |
|---|---|
| `per-test` | Browser opens and closes for every test method (default — full isolation) |
| `per-suite` | Browser stays open for the entire suite; one instance per thread, closed at suite end |

:::tip When to use `per-suite`
Use `per-suite` when your suite has many sequential tests and browser startup time is a bottleneck. The browser retains cookies and state between tests — plan your test flow accordingly.
:::

---

## Execution

### `parallel`
Maps directly to TestNG parallel execution mode. Thread count is set via `threadCount`.

| Value | Behaviour |
|---|---|
| `none` | Sequential execution (default) |
| `methods` | Each `@Test` method runs in its own thread |
| `classes` | Each test class runs in its own thread |
| `tests` | Each `<test>` block in testng.xml runs in its own thread |

### `maxActiveSessions`
Maximum concurrent browser instances. Tests wait (up to 30s) for a slot rather than failing immediately. Prevents resource exhaustion in parallel runs.

---

## Retry

### `enabled`
When `true`, all test methods are retried on failure up to `maxAttempts` times. Set to `false` to disable retry globally.

Use `@Retryable` on a method to override the global setting for that specific test.

```java
@Test
@Retryable(maxAttempts = 3)
public void flakyTest() { ... }
```

---

## Environment profiles

Override the default config for a specific environment using a profile suffix:

```
selenium-boot.yml            ← base config
selenium-boot-staging.yml    ← staging overrides
selenium-boot-prod.yml       ← prod overrides
```

Activate with:

```bash
mvn test -Dselenium.boot.profile=staging
```

Only the fields present in the profile file are overridden — everything else falls back to the base config.
