# Selenium Boot – Pre-Publish Checklist

This document tracks everything that must be completed before the v0.1 public release.
Items are ordered by priority. Blockers must be resolved before any other work ships.

---

## Blockers

### 1. Track test pass/fail/skip status in metrics and report

**Affected files:** `ExecutionMetrics`, `TestTiming`, `TestExecutionListener`, `HtmlReportGenerator`

`ExecutionMetrics` and `TestTiming` only store timing data. `TestExecutionListener` knows
the outcome per test (`onTestSuccess`, `onTestFailure`, `onTestSkipped`) but never records
it. As a result:

- The JSON export has no `status` field per test
- The HTML summary cards show only "Total Tests / Total Time / Avg Time" — no pass/fail/skip breakdown
- The test table has no status column

A test report without pass/fail outcomes is not usable. This is the highest-priority gap.

**Required changes:**
- Add a `status` field to `TestTiming` (values: `PASSED`, `FAILED`, `SKIPPED`)
- Record status in `TestExecutionListener` on each outcome callback
- Add pass/fail/skip counts to the JSON export
- Add a pass/fail/skip summary row to the HTML report header cards
- Add a status column (with color coding) to the HTML test table

---

### 2. Auto-wire RetryListener via IAnnotationTransformer

**Affected files:** `RetryListener`, `BaseTest`, new `RetryAnnotationTransformer`

`RetryListener` implements `IRetryAnalyzer` but TestNG requires it to be declared either
per `@Test(retryAnalyzer = ...)` or injected globally via `IAnnotationTransformer`.
Currently users must manually add `retryAnalyzer = RetryListener.class` to every `@Test`
method — which breaks the zero-configuration promise.

**Required changes:**
- Create `RetryAnnotationTransformer` implementing `IAnnotationTransformer`
- Read `retry.enabled` from config in the transformer; inject `RetryListener` if enabled
- Add `RetryAnnotationTransformer` to the `@Listeners` list in `BaseTest`

---

### 3. Remove or implement all empty stub classes

**Affected files (all `// TODO` stubs):**

| Class | Package |
|---|---|
| `ReportManager` | `reporting` |
| `ReportLifecycle` | `reporting` |
| `ExecutionEngine` | `execution` |
| `ExecutionContext` | `execution` |
| `ParallelExecutionManager` | `execution` |
| `WaitConditions` | `wait` |
| `DriverContext` | `driver` |
| `FrameworkShutdown` | `lifecycle` |

Shipping an empty class with `// TODO: Implementation to be added` inside a published
library is a red flag for users and contributors. Each must be either implemented with
real logic or deleted if it has no role in v0.1.

---

### 4. Apply pageLoad timeout from config to WebDriver instances

**Affected files:** `LocalChromeDriverProvider`, `LocalFirefoxDriverProvider`, `RemoteDriverProvider`

`timeouts.pageLoad` is configurable in YAML and displayed in the HTML report metadata,
but none of the three driver providers ever call:

```java
driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));
```

The config key is currently a dead value. All three providers must apply it after driver creation.

---

### 5. Add maven-surefire-plugin to pom.xml

**Affected file:** `pom.xml`

The current `pom.xml` has no `maven-surefire-plugin` configuration. Without it:

- `mvn test` will not discover TestNG tests reliably
- Suite-level listeners may not fire
- Users running `mvn test` out of the box will hit confusing failures

The plugin must be configured for TestNG. The approach also needs to account for how
end users (who depend on this as a library) will configure their own surefire setup.

---

### 6. Add a LICENSE file

**Affected file:** `README.md`, new `LICENSE` at project root

`README.md` states: *"This project will be released under an open-source license (TBD)"*.
A library cannot be published for public use without a declared license.

**Required changes:**
- Choose a license (Apache 2.0 is the standard for Java OSS)
- Add `LICENSE` file at the project root
- Update the license section in `README.md`

---

## Should Fix Before Publish

### 7. Validate execution.mode accepts only "local" or "remote"

**Affected file:** `ConfigurationLoader`

`ConfigurationLoader.validate()` checks that `execution.mode` is not null but any string
passes. A value like `"foobar"` passes config validation and only fails later when
`DriverProviderFactory` throws an "Unsupported browser" error — a misleading message for
a mode misconfiguration.

**Required change:** Add an explicit check — mode must be `"local"` or `"remote"`.

---

### 8. Fix DriverProviderFactory public constructor

**Affected file:** `DriverProviderFactory`

`DriverProviderFactory` declares `public DriverProviderFactory() {}` but is used
exclusively as a static utility class. Every other utility class in the project
(`DriverManager`, `ConfigurationLoader`, `ExecutionMetrics`, etc.) uses a private
constructor to prevent instantiation. This should match.

**Required change:** Change the constructor to `private`.

---

### 9. Set release version in pom.xml and update README project status

**Affected files:** `pom.xml`, `README.md`

- `pom.xml` version is `1.0-SNAPSHOT` — a development marker, not a publishable release.
  Change to `0.1.0` to match Phase 1 of the roadmap.
- `README.md` project status says *"Early Design / MVP Planning Phase"* — this no longer
  reflects reality. Update to reflect actual implementation state.

---

## Status Summary

| # | Item | Priority | Status |
|---|---|---|---|
| 1 | Test status tracking in metrics and HTML report | Blocker | Done |
| 2 | RetryListener auto-wiring via IAnnotationTransformer | Blocker | Done |
| 3 | Remove / implement empty stub classes | Blocker | Done |
| 4 | Apply pageLoad timeout to WebDriver providers | Blocker | Done |
| 5 | Add maven-surefire-plugin configuration | Blocker | Done |
| 6 | Add LICENSE file | Blocker | Done |
| 7 | Validate execution.mode values | Should Fix | Pending |
| 8 | Fix DriverProviderFactory constructor visibility | Should Fix | Pending |
| 9 | Set release version, update README status | Should Fix | Pending |