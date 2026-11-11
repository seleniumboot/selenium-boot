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
- **Results by Tag** — breakdown of tests, passed, failed, skipped and pass rate per tag (TestNG groups, Cucumber tags, JUnit 5 `@Tag`), with click-to-filter
- **Retry Summary** — retried, recovered, still-failing counts (shown only when retries occurred)
- **Slowest 5 Tests** — ranked by total duration
- **Driver Startup** percentile chart

### Test Cases tab

Full table of all tests with:

| Column | Description |
|---|---|
| Class | Simple class name |
| Test | Test method name and tag chips |
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

## Results by tag

Selenium Boot automatically captures test tags across runners with zero configuration and zero runtime overhead:
- **TestNG**: groups defined on `@Test(groups = {"smoke", "regression"})`
- **Cucumber**: scenario tags defined in `.feature` files (e.g. `@smoke`, `@checkout` — leading `@` is stripped for consistency)
- **JUnit 5**: tags defined via `@Tag("smoke")`

When any test has tags:
1. The **Dashboard tab** displays a **Results by Tag** card listing total tests, passed, failed, skipped, and a visual pass-rate progress bar for each tag.
2. Clicking any tag in the "Results by Tag" table or clicking a tag chip on a test case row navigates directly to the **Test Cases** table, sets an active tag filter pill, and filters the table to matching tests.
3. Tags are exported under the `tags` array for each test in `target/selenium-boot-metrics.json` and as `tag` labels in Allure 2 results.
4. When no tests have tags in a run, the "Results by Tag" card is cleanly omitted.

---

## Failure classification & intelligence

Each failed test is tagged with a high-level triage classification (`Product bug`, `Test bug`, `Environment`, `Flaky`), a fine-grained category, and the evidence for it, worked out from the exception class chain and message the framework already records. It also provides actionable remediation advice ("Action: ...") right out of the box with no AI, no API key, and no network. The triage badge, category, and action callout appear at the top of the failure's detail panel; the raw error and stack trace stay right below.

| Classification | Category | Triggered by |
|---|---|---|
| `Product bug` | `ASSERTION_MISMATCH` | the outermost error is an `AssertionError` (or TestNG/JUnit/`DbAssertException` equivalent), or soft assertions failed |
| `Test bug` | `LOCATOR_NOT_FOUND` | `LocatorException`, `NoSuchElementException`, `NoSuchFrameException` or `NoSuchShadowRootException` anywhere in the cause chain, including a `LocatorException` caused by a `TimeoutException` |
| `Test bug` | `ELEMENT_NOT_INTERACTABLE` | `ElementNotInteractableException`, `ElementClickInterceptedException`, `InvalidElementStateException` |
| `Test bug` | `STALE_ELEMENT` | `StaleElementReferenceException` anywhere in the cause chain |
| `Unknown` | `TIMEOUT` | a `TimeoutException` with no locator or navigation cause |
| `Environment` | `NAVIGATION_ERROR` | `net::ERR_*`, DNS failures, "Timed out receiving message from renderer", page load timeouts, HTTP 5xx in the message |
| `Environment` | `BROWSER_CRASH` | `NoSuchSessionException`, `UnreachableBrowserException`, "invalid session id", page crash |
| `Flaky (Category)` | Any | When retried and passed (`Passed on retry`) or history indicates `Intermittent` execution. Retains the underlying category (e.g. `Flaky (Locator not found)`) |
| `Unknown` | `UNKNOWN` | none of the above. The classifier never guesses |

The classification and action are also written to the metrics JSON as `failureType`, `failureCategory`, `failureEvidence`, and `suggestedAction`, and to the JUnit XML.

For full architecture and design details, see [Failure Intelligence](../guides/failure-intelligence.md).

---

### Failure detail record

Each failed test also gets one bounded `failureDetail` record in `selenium-boot-metrics.json`. The HTML failure panel and optional AI analyzer use that same record, so they do not drift apart. Fields are omitted when the data is unavailable and long values are truncated: text fields and individual list entries to 500 characters, URLs to 2,048 characters, and console/heal lists to 10 entries.

When available, the record contains:

- the failing locator, top application stack frame, and last successful `PASS` step;
- the page URL and title captured at failure time;
- self-healing events with original locator, healed locator, and strategy;
- browser console errors;
- retry/history `testStatus`; and
- same-cause group size plus a representative test and its screenshot/trace paths.

The raw error and full stack trace remain directly below the record in the expanded failure panel.

#### Data sent for AI failure analysis

`ai.failureAnalysis` remains opt-in, bounded by `ai.timeoutSeconds`, and unable to change the suite result. When enabled, the request contains the bounded failure detail above, the rule-based failure category and evidence, the error message, the first 30 stack-trace lines, and the recorded step timeline. This can include application URLs, page titles, locator text, console output, test/class names, and artifact paths. It does not send screenshot or trace file contents. Tests in the same-cause group continue to reuse one analysis.

#### AI run summary

`ai.runSummary: true` (with `ai.apiKey`, independent of `ai.failureAnalysis`) adds one short paragraph to the top of the dashboard: which failure causes account for most failures, which look like product bugs versus test or environment problems, and what to investigate first. It is one API call per run, opt-in, bounded by `ai.timeoutSeconds`, and cannot change the suite result; with no key, no failures, or any error, the card is simply absent.

```yaml
ai:
  runSummary: true
  apiKey: ${CLAUDE_API_KEY}
```

### Choosing an AI provider

The AI features (`ai.failureAnalysis` and `ai.runSummary`) are not tied to one vendor. Set `ai.provider`:

| `ai.provider` | Backend | Default `ai.model` |
|---|---|---|
| `anthropic` (default) | Claude Messages API | `claude-haiku-4-5-20251001` |
| `gemini` | Google Gemini `generateContent` | `gemini-flash-latest` |
| `openai` | Any OpenAI-compatible `/chat/completions` endpoint | `gpt-4o-mini` |

```yaml
ai:
  runSummary: true
  provider: gemini
  apiKey: ${GEMINI_API_KEY}
```

Use `ai.baseUrl` to point `openai` at another compatible server, for example a local Ollama (`http://localhost:11434/v1`) or Azure OpenAI. The same data-minimisation rules apply whichever provider you pick.

The request contains only the run totals and, for the largest same-cause groups (at most 8 groups, 5 test names each), the rule-based category, the error message (first 300 characters) and the test names. It does not send stack traces, steps, page content, or screenshot/trace files. The text is stored in the metrics JSON as `aiRunSummary`.

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

### Share a single test

The full report can run to several megabytes. To share one test, expand it and click
**Download this test** in its detail panel. You get a standalone `<testId>.html` file with
that test's status, error, stack trace, steps and screenshots. It opens offline in any
browser, keeps the report's current light or dark theme, and is small enough to attach
to a Jira ticket, GitHub issue or Slack thread.

The file is built in your browser when you click, so it adds nothing to the report size.

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
