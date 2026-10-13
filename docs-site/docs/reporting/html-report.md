---
description: "Selenium Boot HTML report: a self-contained dashboard with pass-rate gauge, retries, screenshots, flakiness radar, and dark mode, no server needed."
id: html-report
title: Selenium HTML Report
sidebar_label: HTML Report
sidebar_position: 1
---

# HTML Report

Selenium Boot generates a self-contained HTML report at `target/selenium-boot-report.html` after every test run. It requires no server, no extra tools — just open the file in a browser.

---

## Report location

```
target/
└── selenium-boot-report.html   ← open this
```

---

## Tabs

The report has three tabs in the left sidebar:

### Dashboard tab

High-level summary of the run:

- **Total / Passed / Failed / Skipped** counts
- **Duration** — total wall-clock time
- **Pass Rate** — percentage of passing tests, colour-coded (green / orange / red)
- **Retry Summary** — retried, recovered, still-failing counts (shown only when retries occurred)
- **Slowest 5 Tests** — ranked by total duration
- **Driver Startup** percentile chart

### Test Cases tab

Full table of all tests with:

| Column | Description |
|---|---|
| Class | Simple class name |
| Test | Test method name |
| Status | PASSED / FAILED / SKIPPED badge |
| Duration | ms |
| Retries | Badge showing retry count (hidden when 0) |

Click any row to expand the **detail panel**:
- Error message (red, bold)
- Full stack trace (monospace, scrollable)
- Step timeline with timestamps, status badges, and inline screenshots

### Failures tab

Same detail panels as Test Cases tab, but only for failed tests. Detail panels are pre-expanded so you can immediately see what went wrong without clicking.

---

## Failure classification

Each failed test is tagged with a category and the evidence for it, worked out from the exception class chain and message the framework already records. It needs no AI, no API key and no network. The badge and evidence appear at the top of the failure's detail panel; the raw error and stack trace stay right below.

| Category | Triggered by |
|---|---|
| `LOCATOR_NOT_FOUND` | `LocatorException`, `NoSuchElementException`, `NoSuchFrameException` or `NoSuchShadowRootException` anywhere in the cause chain, including a `LocatorException` caused by a `TimeoutException` |
| `ELEMENT_NOT_INTERACTABLE` | `ElementNotInteractableException`, `ElementClickInterceptedException`, `InvalidElementStateException` |
| `STALE_ELEMENT` | `StaleElementReferenceException` anywhere in the cause chain |
| `TIMEOUT` | a `TimeoutException` with no locator or navigation cause |
| `ASSERTION_MISMATCH` | the outermost error is an `AssertionError` (or TestNG/JUnit/`DbAssertException` equivalent), or soft assertions failed |
| `NAVIGATION_ERROR` | `net::ERR_*`, DNS failures, "Timed out receiving message from renderer", page load timeouts, HTTP 5xx in the message |
| `BROWSER_CRASH` | `NoSuchSessionException`, `UnreachableBrowserException`, "invalid session id", page crash |
| `UNKNOWN` | none of the above. The classifier never guesses |

The category is also written to the metrics JSON as `failureCategory` and `failureEvidence`, and to the JUnit XML as the `type` of `<failure>`.

---

### Same-cause groups

When two or more failures share a cause, the Failures tab lists them above the flat list as "N tests, same cause", largest group first, so one outage does not read as forty problems. Tests are grouped by a signature made of the failure category, the exception type, the first line of the message with ids, numbers and quoted values replaced by placeholders, and the top application stack frame (class and method, not line number). The flat list below still shows every failure. With `ai.failureAnalysis` on, one API call is made per group and the other tests in it reuse that analysis.

---

### Retry and history status

Each test is also compared with its own earlier runs in `target/metrics-history/` (the last `flakiness.historyRuns`, the same window the Flakiness Radar uses). A "Test status" card next to the Radar lists the tests that need a second look, and the status appears at the top of the failure's detail panel:

| Status | Meaning |
|---|---|
| **Newly failing** | Failed now; every earlier run in the window passed. Listed first in the Failures tab, since a recent change most likely broke it. |
| **Consistently failing** | Failed now and in at least two earlier runs, with no pass among them. |
| **Intermittent** | Failed now; the earlier runs contain both passes and failures. |
| **Passed on retry** | Failed an attempt in this run, then passed on a retry. |

A test that passed on its first attempt gets no status, and a test with no history gets none either (except passed on retry, which needs no history). Statuses are written to the metrics JSON as `testStatus`. The Radar's STABLE / WATCH / HIGH thresholds and the `failOnHighFlakiness` gate are unchanged.

### Keeping history in CI

The Flakiness Radar and every status except **Passed on retry** need earlier runs. If your build wipes `target/` (`mvn clean`) or the CI workspace is thrown away after each job, there is nothing to compare with and those parts of the report stay empty. Keep the history somewhere that survives, and point Selenium Boot at it:

```yaml
flakiness:
  historyDir: .selenium-boot-history   # relative to the project, or an absolute path
```

or, without touching the config file, `-Dseleniumboot.history.dir=/path/to/history` (the system property wins).

Then persist that directory between runs. With GitHub Actions, for example:

```yaml
- uses: actions/cache@v4
  with:
    path: .selenium-boot-history
    key: selenium-boot-history-${{ github.run_id }}
    restore-keys: selenium-boot-history-
```

On other CI systems, restore the directory from your artifact store before the run and upload it again afterwards. One JSON file is written per run, so the directory stays small; only the newest `flakiness.historyRuns` files are read.

## Self-contained format

All screenshots are Base64-encoded and embedded inline. The report is a single file you can:

- Email to a stakeholder
- Attach to a Jira ticket
- Archive as a CI artifact
- Store in a shared folder

No images folder, no asset references, no server needed.

---

## Step timeline

When tests use `StepLogger`, each step appears in the detail panel:

```
 1  Open login page          +0ms     INFO
 2  Enter credentials         +312ms   INFO
 3  Assert dashboard visible  +891ms   PASS  [screenshot]
```

Thumbnails are clickable — they open full-size in a lightbox overlay.

---

## Configuration

The report path and name are not currently configurable — the file is always written to `target/selenium-boot-report.html`.

---

## CI usage

Upload the report as an artifact to preserve it after the CI workspace is cleaned:

```yaml title="GitHub Actions"
- name: Upload report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: selenium-boot-report
    path: target/selenium-boot-report.html
```

The `if: always()` ensures the report is uploaded even when tests fail.
