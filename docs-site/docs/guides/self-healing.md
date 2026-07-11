---
description: "Self-healing Selenium locators: when a selector breaks, the framework falls back through id, name, text, and data-testid and flags every heal."
id: self-healing
title: Self-Healing Locators
sidebar_position: 10
---

# Self-Healing Locators

When a locator stops matching — because a class was renamed, an `id` moved, or the
markup was refactored — the framework can **automatically retry with alternative
strategies derived from your original locator** instead of failing the test. Every
heal is recorded so you can fix the selector later at your own pace.

Self-healing is **opt-in** and **transparent**: the test continues as if the
locator had worked, and a `⚠ healed` badge appears in the HTML report.

---

## Enabling it

Off by default. Turn it on in `selenium-boot.yml`:

```yaml
locators:
  selfHealing: true
```

---

## How it works

Self-healing hooks into the [`WaitEngine`](./wait-engine). When
`waitForVisible(...)` or `waitForClickable(...)` times out (`TimeoutException` /
`NoSuchElementException`), the framework parses the failing `By` descriptor,
derives an ordered list of fallback locators, and returns the first element that
is found **and visible**. If none match, the original exception is re-thrown —
self-healing never hides a genuinely missing element.

Because it lives in `WaitEngine`, healing applies automatically to anything built
on the framework's waits — `BasePage` actions, semantic locators, and direct
`WaitEngine` calls. You don't call a special API.

### Fallback strategies

Fallbacks are derived from the **original locator's own content** — the framework
never invents selectors out of thin air. Strategies are tried in this order:

| # | Strategy | Derived from | Retries via |
|---|---|---|---|
| 1 | `id-from-css` / `id-from-xpath` | CSS `#foo` or XPath `@id='foo'` | `By.id` |
| 2 | `name-from-css` / `name-from-xpath` | CSS `[name='foo']` or XPath `@name='foo'` | `By.name` |
| 3 | `exact-text-from-xpath` / `contains-text-from-xpath` | XPath `text()='foo'` / `contains(text(),'foo')` | XPath text match |
| 4 | `class-from-css` | last `.className` segment of a compound CSS selector | `By.className` |
| 5 | `data-testid-from-css` | CSS `[data-testid='foo']` | `By.cssSelector` |
| 6 | `placeholder-from-css` | CSS `[placeholder='foo']` | `By.cssSelector` |

**Example.** A locator of `By.cssSelector("div.header input#email")` that no longer
matches will fall back to `By.id("email")`, then to `By.className("header")` — so a
wrapper rename won't break the test as long as the `id` still exists.

---

## Seeing what got healed

Every heal produces a `HealEvent` capturing the test, the original locator, the
locator that worked, and the strategy used. There are two ways to review them.

### HTML report badge

Any test that healed at least one locator is flagged with a `⚠ healed` badge next
to its name in the [HTML report](../reporting/html-report). Hover for the count of
locators auto-healed in that test.

### `target/healed-locators.json`

At suite end the full heal log is exported to `target/healed-locators.json` — one
entry per heal, with the original locator, the healed locator, the strategy, and a
timestamp. Treat this file as a **to-do list of brittle selectors to fix**.

```json
[
  {
    "testId": "LoginTest.loginWithValidCredentials",
    "originalLocator": "By.cssSelector: div.header input#email",
    "healedLocator": "By.id: email",
    "strategy": "id-from-css",
    "timestamp": 1719920400000
  }
]
```

---

## Self-healing vs. SmartLocator

Both make locators more resilient, but they solve different problems:

| | Self-healing (this page) | [`SmartLocator`](./smart-locator) |
|---|---|---|
| When it acts | **After** a locator fails at runtime | **Before** — you supply the candidates up front |
| Who supplies alternatives | Framework derives them from the failing locator | You list them explicitly |
| Opt-in scope | Global (`locators.selfHealing: true`) | Per call site |
| Best for | Catching unexpected drift and logging it for later | Locators you already know differ across environments |

They compose well: use `SmartLocator` where you *anticipate* variation, and leave
self-healing on as a safety net that flags the drift you didn't.

---

## When to use it

| Situation | Enable self-healing? |
|---|---|
| App under active development with churning markup | Yes — keeps CI green while you catch up on selectors |
| You want visibility into which locators are drifting | Yes — the JSON log is your fix list |
| Strict suite where any locator change **should** fail the build | No — leave it off so drift surfaces as failures |

:::tip Don't let healing hide rot
A healed test is a **warning, not a pass to ignore**. Review
`target/healed-locators.json` regularly and update the underlying selectors — a
locator that keeps healing is a locator that will eventually break in a way no
fallback can recover.
:::
