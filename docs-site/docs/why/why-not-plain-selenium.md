---
description: "Why not plain Selenium? Raw Selenium is low-level by design — every team rebuilds the same driver factory, wait utils, retry analyzer, and reporting. Selenium Boot is that framework, maintained and tested, without hiding Selenium."
id: why-not-plain-selenium
title: Why not plain Selenium?
sidebar_label: Why not plain Selenium?
sidebar_position: 2
---

# Why not plain Selenium?

Nothing is wrong with Selenium — Selenium Boot is **built on it** and never hides it. The question isn't "Selenium or not?" It's: *who writes and maintains the scaffolding Selenium deliberately leaves out?*

Raw Selenium hands you a `WebDriver` and stops there. Turning that into a real test suite means building — and then owning forever — the same set of pieces every team rebuilds.

---

## The boilerplate you own with plain Selenium

Almost every Java Selenium project grows its own copy of this:

- A **`DriverFactory`** with `ThreadLocal<WebDriver>` juggling for parallel runs
- Driver-binary management (historically WebDriverManager)
- A **`WaitUtils`** wrapper around `WebDriverWait` / `ExpectedConditions`, imported everywhere
- An **`IRetryAnalyzer`** plus a listener to attach it, for flaky tests
- A screenshot-on-failure **`ITestListener`**
- Reporting wiring (ExtentReports / Allure) and CI glue

It's unpaid infrastructure: you write it, debug it, and maintain it — and it's rarely tested or parallel-safe. It gets **rewritten from scratch at every new project or company.**

---

## What Selenium Boot changes

Selenium Boot **is** that framework — already built, maintained, tested, thread-safe, and documented. You keep the part that's actually yours (test intent) and delete the plumbing:

| Plain Selenium (you build & maintain) | Selenium Boot |
|---|---|
| `DriverFactory` + `ThreadLocal<WebDriver>` | Extend `BaseTest` — lifecycle managed, parallel-safe |
| WebDriverManager / driver binaries | Selenium Manager, automatic |
| `WaitUtils` / `WebDriverWait` helpers | Auto-waiting locators + [`WaitEngine`](/docs/guides/wait-engine) |
| Brittle CSS/XPath selectors | [Accessibility-first locators](/docs/guides/semantic-locators) |
| `IRetryAnalyzer` + listener | [`@Retryable`](/docs/guides/retry) + one config line |
| Screenshot-on-failure listener | Automatic on failure |
| ExtentReports/Allure wiring | [HTML report](/docs/reporting/html-report) + JUnit XML included |
| Bespoke CI wiring per project | Auto-detects GitHub Actions / Jenkins / CircleCI |
| You fix the bugs | The framework ships the fixes |

---

## …without hiding Selenium

The catch with most "productivity layers" is that they take `WebDriver` away and trap you when the abstraction leaks. Selenium Boot doesn't. When the conventions don't fit, `getDriver()` hands you the raw `WebDriver` and you drop straight to `By` / `WebElement`. Opinionated defaults, full escape hatch.

Because it *is* Selenium, adoption is incremental — you can point a single test class at `BaseTest` and a half-migrated suite still runs.

---

## Next steps

- [Migrate from Selenium + TestNG](/docs/migration/from-selenium-testng) — the side-by-side "delete the plumbing" guide
- [Why Selenium Boot?](/docs/why/why-selenium-boot) — the overall philosophy
- [Getting Started](/docs/getting-started) — first test in 5 minutes
