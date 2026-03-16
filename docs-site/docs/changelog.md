---
id: changelog
title: Changelog
sidebar_position: 99
---

# Changelog

All notable changes to Selenium Boot are documented here.

---

## [0.7.0] — 2026-03-16

### Added
- **Browser lifecycle control** — `browser.lifecycle: per-test | per-suite` setting
- **Per-suite driver management** — browser stays open across tests when `per-suite` is configured
- **Step logging** — `StepLogger.step()` API for named test steps with optional screenshots and status badges
- **Step timeline** — Step-by-step execution timeline in the HTML report detail panel
- **Tabbed HTML report** — Left sidebar navigation with Dashboard, Test Cases, and Failures tabs
- **Failures tab** — Pre-expanded failure details for faster debugging
- **Inline step screenshots** — Base64-embedded step screenshots with lightbox on click

### Changed
- HTML report overhauled with tab-based layout and collapsible test rows
- Screenshot lightbox now opens full-size correctly

---

## [0.6.0] — 2025-12-01

### Added
- **Advanced HTML reporting** — Dashboard with metrics cards, test case table, retry summary
- **Donut chart** — Pass/fail/skip distribution chart on Dashboard tab
- **Slowest tests** — Top-5 slowest tests ranked by duration
- **Pass rate gauge** — Colour-coded pass rate percentage card
- **Retry badge** — `↻ Nx` badge on retried tests in the table

### Changed
- Screenshots embedded as Base64 — report is now a single self-contained file

---

## [0.5.0] — 2025-10-15

### Added
- **Retry support** — `retry.enabled` + `retry.maxAttempts` in `selenium-boot.yml`
- **`@Retryable` annotation** — per-method retry override
- **RetryAnnotationTransformer** — auto-registered via Java SPI, zero config
- **Retry metrics** — retry counts tracked in `ExecutionMetrics` and exported to JSON

---

## [0.4.0] — 2025-08-20

### Added
- **`WaitEngine`** — fluent explicit wait API (`waitForVisible`, `waitForClickable`, `waitForText`, etc.)
- **Timeout override** — `getWait(seconds)` for per-call timeout override
- **`waitForStaleness`** — wait for DOM element replacement after AJAX reload

---

## [0.3.0] — 2025-06-10

### Added
- **`BasePage`** — page object base class with `click`, `type`, `getText`, `isDisplayed`, `getAttribute`
- **Parallel execution** — `parallel.enabled` + `parallel.threadCount` configuration
- **Session semaphore** — `browser.maxActiveSessions` cap on concurrent browser instances
- **JUnit XML reporter** — `target/surefire-reports/TEST-SeleniumBoot.xml`

---

## [0.2.0] — 2025-04-05

### Added
- **`BaseTest`** — test base class with `open()`, `open(path)`, `getDriver()`, `getWait()`
- **`SeleniumBootConfig`** — YAML configuration loader (`selenium-boot.yml`)
- **`DriverManager`** — ThreadLocal WebDriver lifecycle management
- **Automatic driver setup** — WebDriverManager integration, no manual driver downloads
- **Headless mode** — `browser.headless: true`
- **Basic HTML report** — pass/fail/skip counts and duration
- **Screenshot on failure** — automatic capture, embedded in report

---

## [0.1.0] — 2025-02-01

### Added
- Initial release
- Chrome and Firefox support
- Basic TestNG integration
- `selenium-boot.yml` configuration file discovery
