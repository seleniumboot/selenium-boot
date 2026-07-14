---
description: "Coming from Playwright to Selenium Boot: a bridge, not a replacement. What's familiar — getByRole/getByLabel locators, auto-waiting, web-first assertThat — and what's genuinely different: architecture, language/runtime, and Selenium Grid."
id: coming-from-playwright
title: Coming from Playwright
sidebar_label: Coming from Playwright
sidebar_position: 3
---

# Coming from Playwright

This is a **bridge, not a migration guide**. Selenium Boot does **not** try to replace Playwright — Playwright is an excellent tool with a genuinely different architecture, and if it's serving your team well, keep using it.

This page is for a different situation: your team lives in the **Selenium / Java / JVM** world — existing suites, Selenium Grid, TestNG, in-house infrastructure — but you admire the ergonomics of Playwright and want them *without leaving that ecosystem*. Selenium Boot brings Playwright's best ideas into Selenium. Here's what will feel familiar, and what is honestly different.

:::info The one-line framing
Selenium Boot is the Spring Boot of Selenium — zero setup, smarter defaults, Playwright-inspired APIs, and enterprise features, without hiding Selenium. It borrows ideas from Playwright; it does not reimplement Playwright.
:::

---

## What's familiar

If you know Playwright's API, a lot of Selenium Boot reads almost the same.

### Accessibility-first locators

Playwright's `getByRole` / `getByLabel` philosophy — target the accessibility tree, not brittle CSS — is a first-class part of Selenium Boot.

```java
// Feels like Playwright:
getByRole(Role.BUTTON).withName("Submit").click();
getByLabel("Email address").type("a@b.com");
getByPlaceholder("Search…").type("boots");
getByText("Forgot password?").click();
getByTestId("checkout-cta").click();
```

| Playwright (JS/TS) | Selenium Boot (Java) |
|---|---|
| `getByRole('button', { name: 'Submit' })` | `getByRole(Role.BUTTON).withName("Submit")` |
| `getByLabel('Email address')` | `getByLabel("Email address")` |
| `getByText('Forgot password?')` | `getByText("Forgot password?")` |
| `getByPlaceholder('Search…')` | `getByPlaceholder("Search…")` |
| `getByTestId('checkout-cta')` | `getByTestId("checkout-cta")` |
| `getByAltText('Logo')` | `getByAltText("Logo")` |
| `getByTitle('Close')` | `getByTitle("Close")` |

See [Accessibility-First Locators](/docs/guides/semantic-locators).

### Auto-waiting

Like Playwright, actions wait for the element to be actionable before interacting — no `Thread.sleep()`, no manually constructed waits.

```java
$("#login").click();   // auto-waits for the element to be clickable
```

When you need an explicit condition, [`WaitEngine`](/docs/guides/wait-engine) gives you a fluent, pre-configured wait — the Selenium equivalent of Playwright's `expect(...).toBeVisible()` waiting.

### Web-first assertions

Playwright's auto-retrying `expect()` has a direct counterpart: `assertThat(...)` retries until the condition is true or the timeout elapses.

```java
assertThat(getByRole(Role.HEADING)).hasText("Welcome back");
assertThat($(".cart-count")).hasText("3");
```

### Convention over configuration

Both tools favour sensible defaults over ceremony. In Selenium Boot you add one dependency, extend `BaseTest`, and driver lifecycle, waits, retries, and reporting are already wired — `selenium-boot.yml` is optional.

---

## What's genuinely different

This is where honesty matters. These are architectural facts, not marketing — they're the reasons Selenium Boot is a *bridge*, not a Playwright clone.

### Architecture

- **Playwright** drives browsers through its own protocol over a persistent connection, with fast, isolated **browser contexts** and features like network interception and auto-managed browser binaries built into that model.
- **Selenium Boot** is built on **Selenium WebDriver** and the **W3C WebDriver protocol**. You get the entire Selenium ecosystem — real `WebDriver`, `By`, `WebElement`, and the escape hatch to drop to raw Selenium anytime — but you inherit WebDriver's model, not Playwright's. Some Playwright niceties (e.g. its context model, its built-in tracing) don't map one-to-one.

### Language and runtime

- **Playwright** is multi-language (JS/TS, Python, .NET, Java) and ships its own patched browser builds that it downloads and manages.
- **Selenium Boot** is **JVM-only** — Java, with TestNG (and an optional JUnit 5 bridge). It runs your **normally installed browsers** (Chrome/Firefox/Edge) via Selenium Manager. This is the point: it fits a Java team's existing stack rather than introducing a new runtime.

### Grid and scaling

- **Playwright** parallelises with its own worker/sharding model.
- **Selenium Boot** uses **Selenium Grid** and TestNG parallelism. If your organisation already runs Grid (or a cloud Selenium provider), Selenium Boot slots straight into it — [parallel execution](/docs/guides/parallel) and [cloud execution](/docs/cloud-execution) are first-class. If you have no Selenium infrastructure and no reason to, that's a point in Playwright's favour, not ours.

---

## When to use which

| Choose… | If… |
|---|---|
| **Playwright** | You're greenfield with no Selenium investment, want its context model / tracing, or your team is happy in Node/Python and its runtime. |
| **Selenium Boot** | Your team is on **Selenium / Java / TestNG**, you run (or want) **Selenium Grid**, and you want Playwright-style ergonomics without abandoning that stack. |

Both are legitimate. Selenium Boot exists so that "we're a Selenium shop" no longer means "we can't have accessibility-first locators, auto-waiting, and web-first assertions."

---

## Next steps

- [Accessibility-First Locators](/docs/guides/semantic-locators) — the `getByRole`/`getByLabel` family
- [WaitEngine](/docs/guides/wait-engine) — explicit waits without `Thread.sleep()`
- [Getting Started](/docs/getting-started) — first test in 5 minutes
- [Migrate from Selenium + TestNG](/docs/migration/from-selenium-testng) — if you're consolidating an existing Selenium suite
