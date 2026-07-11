---
description: "Selenium Boot is the Spring Boot of Selenium: zero setup, Playwright-inspired locators and auto-waiting, and enterprise features, without hiding Selenium."
id: intro
title: Introduction
sidebar_position: 1
slug: /
---

# Selenium Boot

**An opinionated, Spring Boot–inspired Java test automation framework.**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.seleniumboot/selenium-boot)](https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/seleniumboot/selenium-boot/blob/master/LICENSE)

---

## What is Selenium Boot?

Selenium Boot eliminates the boilerplate that every Java Selenium project repeats — WebDriver setup and teardown, wait helpers, retry logic, screenshot capture, and report generation — so your test code contains only test intent.

It is inspired by **Spring Boot's philosophy**: sensible defaults, convention over configuration, and zero required setup for common cases.

```java
public class LoginTest extends BaseTest {

    @Test(description = "Valid user can log in")
    public void loginTest() {
        open();
        new LoginPage(getDriver()).login("admin", "secret");
        Assert.assertTrue(new DashboardPage(getDriver()).isLoaded());
    }
}
```

No `WebDriver` setup. No `@AfterMethod` teardown. No wait helpers. No retry configuration.
**Just the test.**

---

## Design philosophy

People often ask whether Selenium Boot is meant to be an opinionated framework, an extensible toolkit, or a thin productivity layer over Selenium. It's **the Spring Boot of Java test automation** — and the answer is layered, not equal parts of all three:

1. **Opinionated core (primary).** Convention over configuration, zero boilerplate by default. Add one dependency, extend `BaseTest` / `BasePage`, and the framework has already made the sensible decisions for you. `selenium-boot.yml` is optional — `SeleniumBootDefaults` covers you if you never write it.
2. **Never hides Selenium (the constraint).** Unlike heavier abstractions, Selenium Boot never takes the raw `WebDriver` away from you. When the conventions don't fit, drop straight down to `WebDriver` / `By` / `WebElement`. Opinionated without being a cage.
3. **Extensible toolkit (the escape hatch).** An SPI/registry plugin system (`DriverProviderRegistry`, `PluginRegistry`, `ReportAdapterRegistry`) makes it modular for the power users who need it — serving the opinionated core, not replacing it. Most users never touch it.

### Already invested in Selenium?

You don't have to abandon Selenium to get the ergonomics people love in Playwright. Selenium Boot brings those ideas into the Selenium ecosystem — so you keep your stack, your grid, and your team's skills:

| Playwright idea | In Selenium Boot |
|---|---|
| Accessibility-first locators | `getByRole`, `getByLabel`, `getByText`, `getByPlaceholder`, `getByTestId` — target the accessibility tree, survive CSS/DOM refactors |
| Auto-waiting | `WaitEngine`-backed actions — `Thread.sleep()` disappears |
| Web-first assertions | `assertThat(...)` that auto-retries until true |
| Convention over configuration | Zero-boilerplate defaults, optional `selenium-boot.yml` |

…all **without hiding raw Selenium**, and while keeping your existing Selenium / Java / TestNG stack, team skills, and Selenium Grid.

### Why not just build your own framework?

Almost every Java team already has one: a home-grown `BaseTest`, a `DriverFactory`, a pile of wait utilities, and a reporting hack — rewritten from scratch at each new project or company. It's unpaid infrastructure you own, debug, and maintain forever, and it's rarely tested or parallel-safe.

Selenium Boot **is** that framework — already built, maintained, tested, thread-safe, and documented. You keep the part that's actually yours (the test intent) and delete the plumbing:

| Roll your own | Selenium Boot |
|---|---|
| Write & maintain driver lifecycle, waits, retries | Provided, thread-safe, zero config |
| Build a reporting layer from scratch | HTML report + JUnit XML included |
| Bespoke CI wiring per project | Auto-detects GitHub Actions / Jenkins / CircleCI |
| Onboarding = "read our internal wiki" | Onboarding = public docs + one dependency |
| You fix the bugs | The framework ships the fixes |

> Selenium Boot is the Spring Boot of Selenium — zero setup, smarter defaults, Playwright-inspired APIs, and enterprise features, without hiding Selenium.

---

:::tip AI-powered test authoring
**seleniumboot-mcp** lets Claude or GitHub Copilot control a real browser, record your session, and
generate Selenium Boot test code — TestNG, JUnit 5, Page Object, Gherkin, C# NUnit — in one prompt.

```bash
pip install seleniumboot-mcp
```

84 tools · self-healing locators · mobile emulation · codegen for Java / Python / C# / Playwright

[PyPI](https://pypi.org/project/seleniumboot-mcp/) · [GitHub](https://github.com/seleniumboot/selenium-mcp)
:::

---

## What you get out of the box

Outcomes first — the API that delivers each one is on the right so you can jump straight to its docs.

| What you get | How |
|---|---|
| **Never write driver setup or teardown again** | One driver per thread, created before each test, quit after |
| **Switch environments without touching code** | YAML config — browser, parallel, timeouts, retry in one file |
| **Never write `Thread.sleep()` again** | Auto-waiting `WaitEngine` with 10+ built-in conditions |
| **Flaky tests stop failing your build** | Global, per-method `@Retryable`, or per-Cucumber-scenario `@retryable` tag |
| **See exactly why a test failed** | Screenshot auto-captured on failure, base64-embedded in report |
| **Read the test like a spec** | `StepLogger` named steps with optional per-step screenshots |
| **Hand stakeholders a report they'll read** | Tabbed HTML dashboard — overview, test cases, failures, flakiness radar |
| **Plug into any CI without extra tooling** | JUnit XML parsed natively by Jenkins, GitHub Actions, GitLab CI |
| **CI that configures itself** | Headless forced, threads auto-tuned, no config changes needed |
| **Extend it without forking it** | SPI-based plugins — custom drivers, hooks, report adapters |
| **Bring your own test runner** | Full JUnit 5 parity via `@ExtendWith(SeleniumBootExtension.class)` or `BaseJUnit5Test` |
| **Write specs your product team can read** | BDD / Cucumber — `BaseCucumberSteps`, `CucumberHooks`, per-scenario steps in report |
| **Test UI and API in the same suite** | `BaseApiTest`, fluent `ApiClient`, JSONPath, schema validation, hybrid UI+API |
| **Pin down the exact element, fluently** | `$("selector").filter().nth().withText()` — Playwright-style chainable locators |
| **Tests survive CSS and DOM refactors** | Accessibility-first locators — `getByRole(Role.BUTTON).withName("Submit")`, `getByText`, `getByLabel`, `getByPlaceholder`, `getByTestId` |
| **Assertions that don't flake on timing** | Web-first `assertThat(By.id("x")).isVisible()` — auto-retrying until timeout |
| **Test admin-and-user flows in one test** | `withSession("admin", () -> { ... })` — two browsers in one test |
| **Verify what actually landed in the DB** | `db().assertRowExists()`, `db().query().assertValue()` — plain JDBC, no ORM |
| **Assert on the email your app sent** | `mailbox().waitForEmail(to("user@test.com"))` — Mailhog, Mailtrap, Outlook, IMAP |
| **Skip the browser for non-UI tests** | `@NoBrowser` — DB assertions, API checks, file operations, no WebDriver |
| **Run on real cloud browsers unchanged** | `execution.mode: browserstack` / `saucelabs` — session URL in HTML report |
| **Understand failures without digging** | AI failure analysis — Claude explains why a test failed and suggests a fix |
| **Locators that repair themselves** | Self-healing fallback strategies when a locator fails |
| **Know which tests will flake next** | Flakiness prediction — risk scores from run history, radar chart in report |
| **Drive tests from data you already have** | External test data — `@TestData("csv:...")`, `@TestData(value="excel:...", sheet="Login")`, `@TestData("db:SELECT...")` |
| **Test time-dependent behaviour deterministically** | Clock mocking — `clock().set("2030-01-01T00:00:00Z")`, JS `Date` override, auto-reset |

---

## Philosophy

1. **Zero boilerplate** — if the user writes more than 1 line to enable something, it should be a default
2. **Convention over configuration** — smart defaults, YAML opt-in for advanced behaviour
3. **No required external services** — works offline, no cloud APIs in core
4. **Opt-in complexity** — advanced features behind config flags, off by default
5. **Single dependency** — add `selenium-boot` and nothing else is required
6. **Test code stays clean** — internals handle lifecycle; test methods contain only intent

---

## Next steps

- [Getting Started](/docs/getting-started) — install and run your first test in under 5 minutes
- [Configuration Reference](/docs/configuration) — full YAML config documentation
- [Core Guides](/docs/guides/base-test) — deep dives into each feature
