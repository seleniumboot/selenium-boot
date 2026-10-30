---
description: "Coming from Selenide to Selenium Boot: side-by-side mapping of $(), $$, shouldBe/shouldHave and Configuration, plus where Selenide does more."
id: from-selenide
title: Coming from Selenide
sidebar_label: Coming from Selenide
sidebar_position: 4
---

# Coming from Selenide

Selenide is an excellent tool, and if it is working for your team, keep using it. This page is for the team that wants to compare, or is moving a suite, and would like a straight answer to "what does this call look like over there?"

The two share a lot: a short `$("css")` entry point, waiting built into actions and assertions, and no explicit `WebDriverWait` in test code. The difference is where they sit. Selenide wraps WebDriver behind its own `SelenideElement` API. Selenium Boot stays on plain Selenium and adds a test framework around it: driver lifecycle, parallel runs, retries, reporting and cloud execution come from one dependency.

:::info Not a pitch
Selenide does some things Selenium Boot does not. They are listed honestly in [Where Selenide does more](#where-selenide-does-more).
:::

---

## Locating elements

| Selenide | Selenium Boot |
|---|---|
| `$("#login")` | `$("#login")` |
| `$(By.id("login"))` | `$(By.id("login"))` |
| `$$(".item")` | `$(".item")` |
| `$$(".item").get(2)` | `$(".item").nth(2)` |
| `$$(".item").first()` / `.last()` | `$(".item").first()` / `.last()` |
| `$$(".item").size()` | `$(".item").count()` |
| `$$(".item").texts()` | `$(".item").allTextContents()` |
| `$(".row").$(".cell")` | `$(".cell").within(By.cssSelector(".row"))` |
| `$$(".item").filterBy(exactText("Done"))` | `$(".item").withText("Done")` (exact match, trimmed) |
| `$x("//button")` | `$(By.xpath("//button"))` |
| `$(byText("Sign in"))` | `getByText("Sign in")` |

There is no separate `$$`. A `Locator` can match any number of elements; `count()`, `nth()`, `first()`, `last()`, `allTextContents()` and `elements()` work on the whole match, and a terminal action such as `click()` acts on the element you narrowed to.

Selenium Boot also has accessibility-first locators that Selenide's `$` family does not cover directly:

```java
getByRole(Role.BUTTON).withName("Submit").click();
getByLabel("Email address").type("a@b.com");
getByTestId("checkout-cta").click();
```

See [Accessibility-First Locators](/docs/guides/semantic-locators).

---

## Actions

| Selenide | Selenium Boot |
|---|---|
| `$(...).click()` | `$(...).click()` |
| `$(...).doubleClick()` | `$(...).doubleClick()` |
| `$(...).contextClick()` | `$(...).rightClick()` |
| `$(...).setValue("x")` | `$(...).type("x")` |
| `$(...).append("x")` | `$(...).append("x")` |
| `$(...).clear()` | `$(...).clear()` |
| `$(...).pressEnter()` | `$(...).press(Keys.ENTER)` |
| `$(...).hover()` | `$(...).hover()` |
| `$(...).scrollIntoView(true)` | `$(...).scrollIntoView()` |
| `$(...).selectOption("Red")` | `$(...).selectOption("Red")` |
| `$(...).selectOptionByValue("r")` | `$(...).selectOptionByValue("r")` |
| `$(...).setSelected(true)` | `$(...).check()` / `.uncheck()` |
| `$(...).dragAndDropTo(target)` | `$(...).dragTo($(target))` |
| `$(...).getText()` | `$(...).getText()` |
| `$(...).getValue()` | `$(...).inputValue()` |
| `$(...).attr("href")` | `$(...).getAttribute("href")` |
| `$(...).toWebElement()` | `$(...).element()` |

`type()` clears the field before typing, like `setValue()`. Use `append()` to keep existing text.

---

## Assertions

Selenide hangs conditions on the element (`shouldBe`, `shouldHave`). Selenium Boot puts the element inside `assertThat(...)` and uses named methods. Both retry until the condition holds or the timeout elapses.

| Selenide | Selenium Boot |
|---|---|
| `$(...).shouldBe(visible)` | `assertThat($(...)).isVisible()` |
| `$(...).shouldBe(hidden)` | `assertThat($(...)).isHidden()` |
| `$(...).shouldBe(enabled)` | `assertThat($(...)).isEnabled()` |
| `$(...).shouldBe(disabled)` | `assertThat($(...)).isDisabled()` |
| `$(...).shouldBe(checked)` | `assertThat($(...)).isChecked()` |
| `$(...).shouldBe(focused)` | `assertThat($(...)).isFocused()` |
| `$(...).shouldBe(empty)` | `assertThat($(...)).isEmpty()` |
| `$(...).shouldHave(exactText("Hi"))` | `assertThat($(...)).hasText("Hi")` |
| `$(...).shouldHave(text("Hi"))` | `assertThat($(...)).containsText("Hi")` |
| `$(...).shouldHave(value("x"))` | `assertThat($(...)).hasValue("x")` |
| `$(...).shouldHave(attribute("href", "/a"))` | `assertThat($(...)).hasAttribute("href", "/a")` |
| `$(...).shouldHave(cssClass("on"))` | `assertThat($(...)).hasClass("on")` |
| `$$(...).shouldHave(size(5))` | `assertThat($(...)).count(5)` |

`hasText` and `containsText` also accept a `Pattern`. Methods chain: `assertThat($("#msg")).isVisible().containsText("Saved")`.

---

## Configuration

Selenide configures through the `Configuration` class or system properties. Selenium Boot uses one `selenium-boot.yml`, which is optional; with no file it runs on defaults. See the [Configuration Reference](/docs/configuration).

| Selenide | Selenium Boot (`selenium-boot.yml`) |
|---|---|
| `Configuration.browser = "firefox"` | `browser.name: firefox` |
| `Configuration.headless = true` | `browser.headless: true` |
| `Configuration.baseUrl = "https://app"` | `execution.baseUrl: https://app` |
| `Configuration.timeout = 10000` (ms) | `timeouts.explicit: 10` (seconds) |
| `Configuration.pageLoadTimeout = 30000` (ms) | `timeouts.pageLoad: 30` (seconds) |
| `Configuration.remote = "http://grid:4444"` | `execution.mode: remote` and `execution.gridUrl` |
| `Configuration.browserCapabilities` | `browser.capabilities` |

Mind the units: Selenide timeouts are milliseconds, Selenium Boot's are seconds.

```yaml title="selenium-boot.yml"
browser:
  name: chrome
  headless: true
execution:
  baseUrl: https://app.example.com
timeouts:
  explicit: 10
  pageLoad: 30
```

`open("/path")` works as you expect, and resolves against `execution.baseUrl`.

---

## Test structure

| Selenide | Selenium Boot |
|---|---|
| `import static com.codeborne.selenide.Selenide.*` | extend `BaseTest` (or `BasePage` for page objects) |
| `Selenide.open(url)` | `open("/path")`, relative to `execution.baseUrl` |
| Driver managed by Selenide, closed at JVM exit | Driver created and quit per test by the framework |
| `Selenide.switchTo().frame(...)` | [`withinFrame(...)`](/docs/recipes/handle-iframes), which restores context for you |
| `Selenide.sleep(ms)` | Not provided. Wait on a condition instead: see [WaitEngine](/docs/guides/wait-engine). |

`$` and `getBy*` are methods on `BaseTest` and `BasePage`, so a test class or page object extending either can call them directly.

---

## Where Selenide does more

- **Collection conditions.** Selenide's `$$(...).shouldHave(texts("a", "b"))`, `exactTexts(...)` and `sizeGreaterThan(...)` have no one-line equivalent. In Selenium Boot, read the list with `allTextContents()` and assert with ordinary Java or TestNG.
- **Richer condition vocabulary.** Selenide ships many more built-in conditions (CSS values, `be(...)` / `have(...)` negation with `shouldNot`, custom `Condition` subclasses). Selenium Boot's `LocatorAssert` has the methods listed above. For negation, use the paired methods such as `isHidden()` and `isDisabled()`.
- **Selenide's own ecosystem.** Selenide has years of community recipes, an Allure integration and a large body of answered questions. Selenium Boot is younger and smaller.
- **Not tied to a test runner.** Selenide runs under JUnit or TestNG equally. Selenium Boot is built around TestNG, with an optional [JUnit 5 bridge](/docs/junit5).

## Where Selenium Boot does more

- **Selenium stays visible.** `WebDriver`, `By` and `WebElement` are real Selenium types, and `element()` hands you the `WebElement` when you need to drop down.
- **Parallel runs and Grid.** Thread-safe driver handling, `execution.parallel` and `threadCount`, and a session cap are configuration rather than code. See [parallel execution](/docs/guides/parallel).
- **Cloud providers.** BrowserStack, Sauce Labs and LambdaTest are one `execution.mode` value. See [cloud execution](/docs/cloud-execution).
- **Retries, quarantine and reporting.** Retry, flaky-test quarantine and an HTML report with screenshots and a timeline ship in the box. See [retry](/docs/guides/retry) and [HTML report](/docs/reporting/html-report).

---

## Next steps

- [Getting Started](/docs/getting-started): first test in 5 minutes
- [Accessibility-First Locators](/docs/guides/semantic-locators)
- [Migrate from Selenium + TestNG](/docs/migration/from-selenium-testng): the framework plumbing you can delete
