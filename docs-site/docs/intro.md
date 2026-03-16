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

## What you get out of the box

| Feature | Details |
|---|---|
| **Driver lifecycle** | One driver per thread, created before each test, quit after |
| **YAML configuration** | Browser, parallel, timeouts, retry — all in one file |
| **Smart waits** | `WaitEngine` with 10+ built-in conditions |
| **Retry** | Global or per-method, configurable attempts |
| **Screenshots** | Auto-captured on failure, base64-embedded in report |
| **Step logging** | Named steps with optional per-step screenshots |
| **HTML report** | Tabbed dashboard — overview, test cases, failures |
| **JUnit XML** | Parsed natively by Jenkins, GitHub Actions, GitLab CI |
| **CI auto-detection** | Headless forced, threads auto-tuned, no config changes needed |
| **Extensibility** | SPI-based plugins, custom drivers, hooks, report adapters |

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
