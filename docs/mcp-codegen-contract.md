# MCP Codegen Contract

This document is the **contract between the Selenium Boot framework and the
[Selenium Boot MCP server](https://github.com/seleniumboot/selenium-mcp)**.

The MCP server generates framework-native Java from a recorded browser session
(`framework="selenium_boot"`). To do that it hard-codes a mirror of parts of this
framework's public API — the locator factories, the `Role` enum, the base-class
helpers, and the assertion methods. **If you change any API listed here, update
the MCP server's `codegen_tools.py` in the same change**, or generated code will
stop compiling for users.

Everything below is part of the `@SeleniumBootApi` stable surface. Treat a change
here as a breaking change that needs a coordinated MCP release.

---

## 1. Accessibility-first locator factories

The MCP emits these in priority order. Two calling contexts exist:

| Context | Base class | Locator style the MCP emits |
|---|---|---|
| `generate_java_page_object`, `generate_java_testng` | `BasePage` / `BaseTest` | **instance** methods: `getByRole(...)`, `getByLabel(...)`, `$(...)` |
| `generate_java_junit5`, `generate_gherkin` | `BaseJUnit5Test` / `BaseCucumberSteps` | **static** factories: `Locator.byRole(...)`, `Locator.byLabel(...)`, `Locator.of(...)` |

> ⚠️ `BaseJUnit5Test` and `BaseCucumberSteps` expose `$()` / `assertThat()` /
> `open()` but **not** the `getBy*` instance methods. The MCP therefore uses the
> static `Locator.by*` factories there. If you add the `getBy*` helpers to those
> base classes, the MCP can switch them to instance style — until then, keep the
> static factories.

Instance method → static factory equivalence the MCP relies on:

| Instance (BaseTest/BasePage) | Static (Locator) |
|---|---|
| `getByRole(Role.X, "name")` | `Locator.byRole(Role.X).withName("name")` |
| `getByRole(Role.HEADING, "n").withLevel(k)` | `Locator.byRole(Role.HEADING).withName("n").withLevel(k)` |
| `getByLabel("s")` | `Locator.byLabel("s")` |
| `getByText("s")` | `Locator.byText("s")` |
| `getByPlaceholder("s")` | `Locator.byPlaceholder("s")` |
| `getByTestId("s")` | `Locator.byTestId("s")` |
| `getByAltText("s")` | `Locator.byAltText("s")` |
| `getByTitle("s")` | `Locator.byTitle("s")` |
| `$("css")` | `Locator.ofCss("css")` |
| `$(By.x(...))` | `Locator.of(By.x(...))` |

### Locator selection priority (highest → lowest)

The MCP picks the first that applies, from attributes it snapshots off the live
DOM at interaction time:

1. `getByTestId` — `data-testid` / `data-test-id` / `data-test` / `data-cy`
2. `getByRole(Role.BUTTON|LINK|HEADING, name)` — name from `aria-label` / text / `title`
3. `getByLabel` — associated `<label>` (for/wrapping/`aria-labelledby`/`.labels`)
4. `getByPlaceholder` → `getByAltText` → `getByTitle`
5. `$(By.id(...))` → (name attr, low confidence → SmartLocator)
6. selector-based inference, else `$(...)` wrapping the raw `By`

Low-confidence elements (no stable/accessible locator) with ≥2 distinct candidate
strategies fall back to **`smartFind(By primary, By... fallbacks)`** (a `BasePage`
method — page objects only).

## 2. `Role` enum

The MCP mirrors this enum in `codegen_tools._ROLE_ENUM` and `_role_enum_from`.
It currently maps these to `Role.*`: `BUTTON, LINK, CHECKBOX, RADIO, SWITCH,
TEXTBOX, SEARCHBOX, COMBOBOX, OPTION, HEADING, IMG, TAB, MENUITEM, SLIDER,
SPINBUTTON`. Only `BUTTON`, `LINK`, `HEADING` are emitted with an accessible
name today. The authoritative list of roles lives in
[`Role.java`](../src/main/java/com/seleniumboot/locator/Role.java) — if you add or
rename a role that the MCP should target, update `_ROLE_ENUM`.

## 3. `Locator` terminal actions used by generated code

`type(String)`, `click()`, `hover()`, `scrollIntoView()`, `element()` (→
`WebElement`, used to bridge to `Actions`/`Select` for double/right-click and
`<select>`), `withName(String)`, `withLevel(int)`, `toBy()` (page objects bridge
to `BasePage` helpers via this). Removing or renaming any of these breaks codegen.

## 4. `assertThat(...)` — web-first assertions

The MCP emits `assertThat(<locator>)` chained with:
`isVisible()`, `isHidden()`, `hasText(String)`, `containsText(String)`,
`hasAttribute(String, String)`, `count(int)`.
Page-title / URL checks fall back to `org.testng.Assert` (TestNG flavors) or
`org.junit.jupiter.api.Assertions` (JUnit 5) on `getDriver().getTitle()` /
`getCurrentUrl()`. See [`LocatorAssert.java`](../src/main/java/com/seleniumboot/assertion/LocatorAssert.java).

## 5. Base-class helpers the generated code calls

| Base class | Helpers the MCP emits |
|---|---|
| `BaseTest` | `open()`, `open(String)`, `getDriver()`, `getBy*`, `$`, `assertThat` |
| `BasePage` | `super(driver)`, `type/click/hover/scrollTo`, `doubleClick/rightClick(By)`, `selectByText/selectByValue(By, ...)`, `smartFind(By, By...)`, `getBy*`, `$`, `assertThat` |
| `BaseJUnit5Test` | `open`, `getDriver`, `$`, `assertThat` (static `Locator.by*` for a11y) |
| `BaseCucumberSteps` | `open`, `getDriver`, `$`, `assertThat` (static `Locator.by*` for a11y) |

## 6. `open(path)` semantics

Generated tests call `open("/path")`, which resolves against
`execution.baseUrl` in `selenium-boot.yml`. The MCP strips the origin from the
recorded absolute URL; a cross-origin navigation falls back to
`getDriver().get(absoluteUrl)`. Keep `open(String)` resolving relative to
`execution.baseUrl`.

---

**When you touch the public API, grep the MCP repo's `codegen_tools.py` for the
symbol before merging.** The MCP's `detect_selenium_boot` tool keys off
`selenium-boot.yml` and the `io.github.seleniumboot` coordinates — keep those
stable too.
