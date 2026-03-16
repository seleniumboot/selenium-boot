---
id: html-report
title: HTML Report
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
