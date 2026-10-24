---
description: "Pass-rate and duration trend charts in the Selenium Boot HTML report: where the history comes from, the 20-run window, and how to keep it across CI runs."
id: dashboard-trends
title: Dashboard Trend Charts
sidebar_label: Trend Charts
sidebar_position: 3
---

# Dashboard Trend Charts

Selenium Boot's HTML report includes pass-rate and test-duration trend charts to help you understand how your test suite changes across runs.

## Data Source

The trend charts use historical test metrics stored as JSON files in the metrics-history directory. `HtmlReportGenerator.buildTrendData()` reads these files, sorts them by filename, and prepares the data for the HTML report.

- **Pass-rate trend:** Shows how the test pass rate changes across test runs.
- **Duration trend:** Shows how test execution time changes across test runs.

## Configuring Metrics History

The trend charts depend on persisted metrics history. You can configure the history directory using `flakiness.historyDir` or the JVM system property `-Dseleniumboot.history.dir`.

The history directory is resolved in the following order:

1. **JVM system property:** `-Dseleniumboot.history.dir=/path/to/history` takes precedence when the property is nonblank.
2. **Flakiness configuration:** If the JVM property is absent or blank, Selenium Boot uses `flakiness.historyDir` when configured.
3. **Default directory:** If neither setting provides a directory, Selenium Boot uses `metrics-history` under its base directory.

Keep the history directory available across test runs so that reports can include metrics from previous executions.

## The 20-Run Window

The trend charts display up to the 20 most recent runs. The report generator calls `buildTrendData(20)` to limit the history included in the charts.

History files are sorted by filename, and only the latest 20 files are selected when more than 20 are available. Older runs fall outside the selected window as new history files are added.

## Why Two Runs Are Required

A trend requires data from multiple test runs. With only one run, the report has no previous run to compare against.

The trend charts remain hidden until metrics from at least two runs are available. Once history contains two or more runs, the charts can show changes in pass rate and duration.

## Preserving History in CI

CI jobs often use fresh workspaces or disposable containers. If the metrics-history directory is lost after each run, later reports cannot use the earlier metrics to display trends.

To preserve history across CI runs:

1. Configure a consistent metrics-history directory.
2. Restore the previous history directory before running the tests.
3. Execute the tests and generate the HTML report.
4. Save the updated history directory as a CI artifact or in persistent storage.
5. Restore the saved history before the next test run.

A CI cache can also be used, but caches may be evicted. Use retained artifacts or persistent storage when reliable history is required.

Ensure that each run's metrics-history JSON files remain available to subsequent runs so the report can build a meaningful trend.
