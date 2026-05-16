---
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

:::tip AI-powered test authoring
**seleniumboot-mcp** lets Claude or GitHub Copilot control a real browser, record your session, and
generate Selenium Boot test code — TestNG, JUnit 5, Page Object, Gherkin, C# NUnit — in one prompt.

```bash
pip install seleniumboot-mcp
```

76 tools · self-healing locators · mobile emulation · codegen for Java / Python / C# / Playwright

[PyPI](https://pypi.org/project/seleniumboot-mcp/) · [GitHub](https://github.com/seleniumboot/selenium-mcp)
:::

---

## What you get out of the box

| Feature | Details |
|---|---|
| **Driver lifecycle** | One driver per thread, created before each test, quit after |
| **YAML configuration** | Browser, parallel, timeouts, retry — all in one file |
| **Smart waits** | `WaitEngine` with 10+ built-in conditions |
| **Retry** | Global, per-method `@Retryable`, or per-Cucumber-scenario `@retryable` tag |
| **Screenshots** | Auto-captured on failure, base64-embedded in report |
| **Step logging** | Named steps with optional per-step screenshots |
| **HTML report** | Tabbed dashboard — overview, test cases, failures, flakiness radar |
| **JUnit XML** | Parsed natively by Jenkins, GitHub Actions, GitLab CI |
| **CI auto-detection** | Headless forced, threads auto-tuned, no config changes needed |
| **Extensibility** | SPI-based plugins, custom drivers, hooks, report adapters |
| **JUnit 5** | Full feature parity via `@ExtendWith(SeleniumBootExtension.class)` or `BaseJUnit5Test` |
| **BDD / Cucumber** | `BaseCucumberSteps`, `CucumberHooks`, per-scenario steps in HTML report |
| **API testing** | `BaseApiTest`, fluent `ApiClient`, JSONPath, schema validation, hybrid UI+API |
| **Fluent locators** | `$("selector").filter().nth().withText()` — Playwright-style chainable locators |
| **Web-first assertions** | `assertThat(By.id("x")).isVisible()` — auto-retrying until timeout |
| **Multi-session testing** | `withSession("admin", () -> { ... })` — two browsers in one test |
| **Database assertions** | `db().assertRowExists()`, `db().query().assertValue()` — plain JDBC, no ORM |
| **Email verification** | `mailbox().waitForEmail(to("user@test.com"))` — Mailhog, Mailtrap, Outlook, IMAP |
| **`@NoBrowser`** | Skip WebDriver for non-UI tests — DB assertions, API checks, file operations |
| **BrowserStack / Sauce Labs** | `execution.mode: browserstack` or `saucelabs` — cloud browsers with session URL in HTML report |
| **AI failure analysis** | Claude explains why a test failed and suggests a fix |
| **Self-healing locators** | Automatic fallback strategies when a locator fails |
| **Flakiness prediction** | Risk scores from run history, radar chart in report |
| **External test data** | `@TestData("csv:...")`, `@TestData(value="excel:...", sheet="Login")`, `@TestData("db:SELECT...")` |
| **Clock mocking** | `clock().set("2030-01-01T00:00:00Z")` — JS `Date` override, auto-reset after each test |

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
