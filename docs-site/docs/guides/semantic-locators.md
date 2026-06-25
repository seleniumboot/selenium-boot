---
id: semantic-locators
title: Accessibility-First Locators
sidebar_position: 10
---

# Accessibility-First Locators

Selenium Boot ships Playwright-style **semantic locators** that target the
**accessibility tree** — what the user actually perceives — instead of brittle
CSS classes or DOM structure. The result: locators that read like the page and
survive redesigns.

```java
getByRole(Role.BUTTON).withName("Submit").click();
getByLabel("Email address").type("a@b.com");
getByPlaceholder("Search…").type("boots");
getByText("Forgot password?").click();
getByTestId("checkout-cta").click();
```

Every semantic locator returns the same chainable, **auto-waiting** `Locator`
used by the `$()` API — no `Thread.sleep`, no explicit waits. They're available
on both `BaseTest` and `BasePage`.

---

## Why semantic locators?

```java
// Brittle — breaks the moment the markup is refactored:
$(By.cssSelector("div.modal > form button.btn-primary")).click();

// Resilient — targets the role + accessible name the user sees:
getByRole(Role.BUTTON).withName("Submit").click();
```

---

## The locators

| Method | Matches |
|---|---|
| `getByRole(Role)` | Elements by ARIA role (implicit HTML element **or** explicit `role="…"`) |
| `getByText(String)` | Elements by visible text |
| `getByLabel(String)` | Form controls by their associated `<label>` text |
| `getByPlaceholder(String)` | Elements by `placeholder` attribute |
| `getByTestId(String)` | Elements by test-id attribute (default `data-testid`) |
| `getByAltText(String)` | Elements (typically `<img>`) by `alt` text |
| `getByTitle(String)` | Elements by `title` attribute |

---

## `getByRole`

`Role` covers 38 WAI-ARIA roles. Each matches both the native HTML elements that
carry the role implicitly and any element with an explicit `role` attribute —
e.g. `Role.BUTTON` matches `<button>`, `<input type="submit">`, `<summary>`, and
`[role="button"]`.

```java
getByRole(Role.BUTTON).withName("Save").click();   // accessible-name match
getByRole(Role.LINK, "Docs").click();              // role + name in one call
getByRole(Role.HEADING).withLevel(1).getText();    // heading level
```

`.withName(...)` matches the element's **accessible name**, computed following
ARIA precedence: `aria-label` → `aria-labelledby` → associated `<label>` →
text content → `value` / `alt` / `title`.

---

## Exact vs. substring

Text, name, and attribute matching is **case-insensitive substring** by default.
Call `.exact()` for a case-sensitive exact match:

```java
getByText("submit");          // matches "Submit", "SUBMIT ORDER", …
getByText("Submit").exact();  // matches only "Submit"
```

---

## Configuring the test-id attribute

`getByTestId` uses `data-testid` by default. Override it in `selenium-boot.yml`:

```yaml
locators:
  testIdAttribute: data-qa
```

Or programmatically: `Locator.setTestIdAttribute("data-qa");`

---

## Escape hatch: `toBy()`

Every semantic locator can hand back its synthesized Selenium `By` for interop
with raw Selenium or [`SmartLocator`](./smart-locator):

```java
By submitBtn = getByRole(Role.BUTTON).toBy();
WebElement el = driver.findElement(submitBtn);
```

> `toBy()` returns the **base selector**. Refinements that can't be expressed as
> a `By` (e.g. `.withName(...)`) are applied only by the terminal actions
> (`click()`, `type()`, …) on the fully-resolved element.
