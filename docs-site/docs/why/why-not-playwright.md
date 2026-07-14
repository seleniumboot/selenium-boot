---
description: "Why not Playwright? An honest answer: Selenium Boot does not replace Playwright. It brings Playwright's best ideas — accessibility-first locators, auto-waiting, web-first assertions — to teams staying in the Selenium / Java / Grid ecosystem."
id: why-not-playwright
title: Why not Playwright?
sidebar_label: Why not Playwright?
sidebar_position: 3
---

# Why not Playwright?

Let's be honest up front: **Selenium Boot does not replace Playwright.** Playwright is an excellent tool with a genuinely different architecture, and if you're greenfield with no Selenium investment, it's a great choice.

So "Why not Playwright?" isn't a takedown — it's a question with a real answer for a specific audience: **teams already in the Selenium / Java / JVM world** who admire Playwright's ergonomics but don't want to abandon their stack, their Selenium Grid, or their team's skills to get them.

---

## The ergonomics you don't have to give up

You don't have to leave Selenium to get the things people love about Playwright. Selenium Boot brings those ideas into the Selenium ecosystem:

| Playwright idea | In Selenium Boot |
|---|---|
| Accessibility-first locators | [`getByRole` / `getByLabel` / `getByText`](/docs/guides/semantic-locators) — target the accessibility tree, survive CSS/DOM refactors |
| Auto-waiting | Auto-waiting actions + [`WaitEngine`](/docs/guides/wait-engine) — `Thread.sleep()` disappears |
| Web-first assertions | `assertThat(...)` that auto-retries until true |
| Convention over configuration | Zero-boilerplate defaults, optional `selenium-boot.yml` |

…all while keeping your existing **Selenium / Java / TestNG** stack and **Selenium Grid**, and without ever hiding raw Selenium.

---

## Where Playwright is genuinely different

Honesty cuts both ways. These are real architectural differences, not marketing:

- **Architecture** — Playwright drives browsers over its own protocol with fast, isolated browser contexts and built-in tracing. Selenium Boot is built on **Selenium WebDriver / the W3C protocol** — you get the whole Selenium ecosystem, but WebDriver's model, not Playwright's.
- **Language & runtime** — Playwright is multi-language and ships its own managed browser builds. Selenium Boot is **JVM-only** and runs your normally installed browsers.
- **Scaling** — Playwright has its own worker/sharding model; Selenium Boot uses **Selenium Grid** and TestNG parallelism, slotting into infrastructure you may already run.

For the full familiar-vs-different breakdown, see the bridge page: [Coming from Playwright](/docs/migration/coming-from-playwright).

---

## Choosing honestly

| Choose… | If… |
|---|---|
| **Playwright** | Greenfield, no Selenium investment, want its context model / tracing, team happy in Node/Python. |
| **Selenium Boot** | You're on **Selenium / Java / TestNG**, run (or want) **Selenium Grid**, and want Playwright-style ergonomics without leaving that stack. |

Both are legitimate. Selenium Boot exists so that "we're a Selenium shop" no longer means giving up accessibility-first locators, auto-waiting, and web-first assertions.

---

## Next steps

- [Coming from Playwright](/docs/migration/coming-from-playwright) — the full bridge page
- [Accessibility-First Locators](/docs/guides/semantic-locators) — the `getByRole`/`getByLabel` family
- [Why Selenium Boot?](/docs/why/why-selenium-boot) — the overall philosophy
