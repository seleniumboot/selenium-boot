---
description: "Why Selenium Boot: the Spring Boot of Selenium — zero setup, Playwright-inspired locators and auto-waiting, and enterprise features, without hiding Selenium. The philosophy before the API."
id: why-selenium-boot
title: Why Selenium Boot?
sidebar_label: Why Selenium Boot?
sidebar_position: 1
---

# Why Selenium Boot?

Developers adopt a philosophy before they adopt an API. This page is the philosophy.

> Selenium Boot is the Spring Boot of Selenium — zero setup, smarter defaults, Playwright-inspired APIs, and enterprise features, without hiding Selenium.

Selenium is powerful and everywhere, but it's deliberately low-level: it gives you a `WebDriver` and gets out of the way. Everything else — driver lifecycle, waits, retries, reporting, CI wiring — is left to you. Every team ends up rebuilding the same scaffolding. Selenium Boot **is** that scaffolding, done once, properly.

---

## The idea in three layers

Selenium Boot is opinionated, but not a cage. The design is layered, not equal parts:

1. **Opinionated core (primary).** Convention over configuration, zero boilerplate by default. Add one dependency, extend `BaseTest` / `BasePage`, and the framework has already made the sensible decisions — driver lifecycle, waits, retries, reporting, CI wiring. `selenium-boot.yml` is optional; sensible defaults cover you if you never write it.
2. **Never hides Selenium (the constraint).** Unlike heavier abstractions, Selenium Boot never takes the raw `WebDriver` away from you. When the conventions don't fit, drop straight to `WebDriver` / `By` / `WebElement`. Opinionated without being a cage.
3. **Extensible toolkit (the escape hatch).** An SPI/registry plugin system makes it modular for power users — custom drivers, report adapters, lifecycle hooks. Most users never touch it.

---

## What you get for one dependency

Outcomes first — with the API that delivers them:

| Outcome | How |
|---|---|
| **Never write `Thread.sleep()` again** | Auto-waiting locators + [`WaitEngine`](/docs/guides/wait-engine) |
| **Tests survive CSS refactors** | [Accessibility-first locators](/docs/guides/semantic-locators) — `getByRole`, `getByLabel`, `getByText` |
| **Flaky tests recover automatically** | [`@Retryable`](/docs/guides/retry) + one config line — no `IRetryAnalyzer` wiring |
| **See exactly why a test failed** | Screenshot auto-captured on failure, embedded in the [HTML report](/docs/reporting/html-report) |
| **No WebDriver binaries to manage** | Selenium Manager resolves drivers automatically |
| **Parallel-safe out of the box** | ThreadLocal driver isolation — [parallel execution](/docs/guides/parallel) |
| **Extend it without forking it** | Java SPI plugins for drivers, reports, hooks |

---

## Who it's for

- **Teams already invested in Selenium** who want Playwright-style ergonomics without leaving the Selenium / Java / TestNG stack, their team's skills, or Selenium Grid.
- **Teams maintaining a home-grown framework** — a `BaseTest`, a `DriverFactory`, a pile of wait utilities — who'd rather delete that plumbing than keep debugging it.

The two "why" questions those teams actually ask each have their own page:

- [Why not plain Selenium?](/docs/why/why-not-plain-selenium) — what the boilerplate really costs
- [Why not Playwright?](/docs/why/why-not-playwright) — an honest comparison (we don't claim to replace it)

---

## Next steps

- [Getting Started](/docs/getting-started) — first test in 5 minutes
- [BaseTest](/docs/guides/base-test) / [BasePage](/docs/guides/base-page) — the base classes you extend
- [Configuration Reference](/docs/configuration) — the full `selenium-boot.yml`
