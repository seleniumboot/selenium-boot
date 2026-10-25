---
description: "Self-contained HTML trace viewer per test with clickable step timeline, per-step screenshots, failure snapshot, error details, and GitHub Actions summary integration."
id: trace-viewer
title: Trace Viewer
sidebar_label: Trace Viewer
sidebar_position: 9
---

# Trace Viewer

Selenium Boot generates a standalone, self-contained HTML trace viewer per test with an interactive step timeline, per-step screenshots, final-state failure snapshot, and full error details — with zero external CDN dependencies.

---

## Enabling Trace Recording

Trace recording is configured in `selenium-boot.yml`. When `tracing.enabled` is not set, traces for **failed tests are recorded automatically in CI** (detected from `CI`, `GITHUB_ACTIONS`, `JENKINS_URL` and similar) and stay off on your machine. An explicit `enabled: true` or `enabled: false` always wins.

```yaml title="selenium-boot.yml"
tracing:
  enabled: true
  captureOnPass: false  # set true to generate traces for passing tests too
  captureNetwork: false # set true to list failed requests (4xx/5xx, network errors) in the trace
```

When enabled, a self-contained HTML file is generated for each failed test at:

```
target/traces/{ClassName}/{testMethod}-trace.html
```

(Or `build/traces/...` in Gradle projects, honoring `-Dseleniumboot.reports.dir`).

---

## What the Trace Viewer captures

Each trace file is an independent, single-file HTML document featuring:

1. **Metadata header** — test method name, class, browser name, duration in milliseconds, status badge, and timestamp.
2. **Interactive step timeline** — every named step recorded via `StepLogger.step(...)` appears in the timeline with its offset. Steps that captured screenshots display a camera icon (📷) and open the interactive screenshot panel on click.
3. **Final-state screenshot** — full-browser screenshot captured automatically at the exact moment of test failure.
4. **Locators used** — each `Locator` action (click, type, getText, …) listed under the step it ran in; a lookup that timed out is marked "not found".
5. **Console errors** — with `browser.captureConsoleErrors: true`, JS errors are listed under the step they happened in (Chrome; other browsers list them without a step).
6. **Failed requests** — with `tracing.captureNetwork: true`, requests that returned 4xx/5xx or failed outright (method, URL, status or error) are listed under their step. Uses CDP on Chrome/Edge and WebDriver BiDi on Firefox; off by default because it keeps a listener open for the whole test. Successful requests are not recorded, and at most 200 are kept per test.
7. **Failure details** — highlighted error message and full scrollable stack trace.
8. **Zero CDN dependencies** — CSS styles and interactive JavaScript are completely inlined so traces can be viewed offline, archived, or shared without broken assets.

---

## Viewing traces

### From the HTML report
In the main Selenium Boot HTML report (`target/selenium-boot-report.html`), every failed test in the Failures tab features a **View Trace** link in its failure detail panel that opens the corresponding trace file.

### Directly from the file system
Open `target/traces/{ClassName}/{testMethod}-trace.html` in any modern web browser.

### In GitHub Actions
When running in GitHub Actions and `tracing.enabled: true` is on, failed tests are automatically summarized in a Markdown table appended to `$GITHUB_STEP_SUMMARY`:

| Test | Error | Trace |
| --- | --- | --- |
| `com.example.CartTest.testCheckout` | `Timeout waiting for element` | `target/traces/CartTest/testCheckout-trace.html` |

This lets teams triage failures and locate the corresponding HTML trace file directly on the Actions job summary page without downloading and extracting artifact archives.

The summary also links to the workflow run page, where artifacts are listed. Upload the traces in your workflow so the link leads somewhere:

```yaml
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: traces
          path: target/traces/
```

