---
description: "Migrate a Selenium + TestNG framework to Selenium Boot: delete your driver factory, wait utils, retry analyzer, and reporting glue, and see the boilerplate disappear side by side."
id: from-selenium-testng
title: Migrate from Selenium + TestNG
sidebar_label: From Selenium + TestNG
sidebar_position: 1
---

# Migrate from Selenium + TestNG

If you already run a hand-rolled **Selenium + TestNG** framework, you have written — and now maintain — a driver factory, a waits utility, a retry analyzer, screenshot-on-failure glue, and a reporting integration. Selenium Boot ships all of that as one dependency.

This guide is a side-by-side **"your current setup → Selenium Boot equivalent."** The short version: most of the plumbing you maintain today simply gets deleted.

:::info Nothing to relearn
Selenium Boot is still Selenium. `WebDriver`, `By`, `WebElement`, and your existing page-object patterns all still work — you're removing boilerplate, not switching tools.
:::

---

## Setup — swap dependencies

Remove your Selenium, WebDriverManager, and reporting dependencies and add one:

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>3.2.0</version>
</dependency>
```

Selenium Boot brings Selenium (and TestNG) transitively. You no longer declare `selenium-java`, `webdrivermanager`, or a reporting library yourself.

Then create a small [`selenium-boot.yml`](/docs/configuration) — see [config mapping](#config-mapping) below.

---

## 1. Driver setup

**Before** — a driver factory, `ThreadLocal` juggling for parallel runs, and `WebDriverManager` to fetch binaries:

```java title="DriverFactory.java (delete this)"
public class DriverFactory {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    public static void createDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        DRIVER.set(new ChromeDriver(options));
        DRIVER.get().manage().timeouts()
              .implicitlyWait(Duration.ofSeconds(10));
    }

    public static WebDriver getDriver() { return DRIVER.get(); }

    public static void quitDriver() {
        DRIVER.get().quit();
        DRIVER.remove();
    }
}

public class BaseTest {
    @BeforeMethod public void setUp()    { DriverFactory.createDriver(); }
    @AfterMethod  public void tearDown() { DriverFactory.quitDriver(); }
}
```

**After** — extend `BaseTest`. Driver creation, per-thread isolation, and teardown are handled for you:

```java title="LoginTest.java"
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    public void loginTest() {
        open();  // navigates to execution.baseUrl
        // ...
    }
}
```

- **No `WebDriverManager`.** Modern Selenium (4.6+) bundles **Selenium Manager**, which downloads the right driver binary automatically. Selenium Boot uses it — delete the `.setup()` calls and the dependency. See [Migrate from WebDriverManager](/docs/migration/from-webdrivermanager) for the details.
- **No `ThreadLocal`.** `DriverManager` isolates the driver per thread, so [parallel runs](/docs/guides/parallel) are safe out of the box.
- Need the raw driver? It's still there: `getDriver()`.

:::caution Drop the implicit wait
Delete `implicitlyWait(...)`. Selenium Boot's locators auto-wait explicitly; mixing implicit and explicit waits is a classic source of flaky, slow tests.
:::

---

## 2. Waits

**Before** — a `WaitUtils` helper wrapping `WebDriverWait` / `ExpectedConditions`, imported into every page:

```java title="WaitUtils.java (delete this)"
public class WaitUtils {
    public static WebElement waitVisible(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    public static void waitClickable(WebDriver driver, By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(locator));
    }
}

// usage
WaitUtils.waitVisible(driver, By.id("login")).click();
```

**After** — every locator **auto-waits**, and `WaitEngine` (pre-configured from your `timeouts.explicit`) covers the explicit cases:

```java
$("#login").click();                          // auto-waits for clickable
getWait().waitForInvisible(By.cssSelector(".spinner"));
getWait().waitForText(By.cssSelector("h1"), "Welcome back");
```

No `Thread.sleep()`, no per-page `WebDriverWait` construction, no passing `driver` around. See the [WaitEngine guide](/docs/guides/wait-engine).

---

## 3. Retry / flaky tests

**Before** — an `IRetryAnalyzer` plus a listener to attach it to every method:

```java title="RetryAnalyzer.java + RetryListener.java (delete both)"
public class RetryAnalyzer implements IRetryAnalyzer {
    private int count = 0;
    private static final int MAX = 2;
    @Override public boolean retry(ITestResult result) {
        return count++ < MAX;
    }
}

public class RetryListener implements IAnnotationTransformer {
    @Override public void transform(ITestAnnotation ann, Class c,
                                    Constructor ctor, Method m) {
        ann.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
// + register the listener in testng.xml
```

**After** — one config line turns on retry for the whole suite:

```yaml title="selenium-boot.yml"
retry:
  enabled: true
  maxAttempts: 2   # total attempts including the first run
```

Override per test with `@Retryable` when you need to:

```java
@Test
@Retryable(maxAttempts = 3)
public void flakyTest() { /* ... */ }
```

Recovered vs. still-failing retries are broken out in the report. See the [Retry guide](/docs/guides/retry).

---

## 4. Screenshots on failure

**Before** — an `ITestListener` that reaches into the driver on `onTestFailure`, encodes a PNG, and writes it somewhere your report can find:

```java title="ScreenshotListener.java (delete this)"
public class ScreenshotListener implements ITestListener {
    @Override public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverFactory.getDriver();
        File png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        // ...copy to /screenshots, attach to report, handle IOException...
    }
}
```

**After** — nothing. Selenium Boot captures a screenshot on every failure automatically and embeds it in the HTML report. Delete the listener. See [Screenshots](/docs/guides/screenshots).

---

## 5. Reporting

**Before** — wire in ExtentReports/Allure: a listener, a `flush()` in an `@AfterSuite`, and per-test logging calls scattered through your code.

**After** — a self-contained **HTML report** at `target/selenium-boot-report.html` (pass-rate gauge, retries, embedded screenshots, flakiness) and a **JUnit XML** file for CI, both generated automatically after every run. Add named steps with the optional [Step Logging](/docs/guides/step-logging) API if you want richer reports.

See [HTML Report](/docs/reporting/html-report) and [JUnit XML](/docs/reporting/junit-xml).

---

## What gets deleted

| Your current setup | Selenium Boot |
|---|---|
| `DriverFactory` + `ThreadLocal<WebDriver>` | ✅ Built in — extend `BaseTest` |
| `WebDriverManager.chromedriver().setup()` | ✅ Selenium Manager (automatic) |
| Implicit-wait config | ✅ Auto-waiting locators |
| `WaitUtils` / `WebDriverWait` helpers | ✅ `WaitEngine` + auto-wait |
| `IRetryAnalyzer` + `IAnnotationTransformer` | ✅ `retry:` config + `@Retryable` |
| Screenshot-on-failure `ITestListener` | ✅ Automatic on failure |
| ExtentReports/Allure wiring | ✅ HTML report + JUnit XML |
| `@BeforeMethod` / `@AfterMethod` lifecycle glue | ✅ Framework-managed lifecycle |

Your page objects and `@Test` methods stay — they just get shorter.

---

## Config mapping

Settings that lived in `testng.xml` attributes and scattered constants move into one file:

```yaml title="selenium-boot.yml"
execution:
  baseUrl: https://your-app.com
  parallel: methods        # was: <suite parallel="methods">
  threadCount: 4           # was: thread-count="4"

browser:
  name: chrome
  headless: false          # auto-forced true when CI is detected

timeouts:
  explicit: 10             # was: your WaitUtils constant
  pageLoad: 30

retry:
  enabled: true
  maxAttempts: 2           # was: RetryAnalyzer MAX
```

You still keep a minimal `testng.xml` to list your test classes — Selenium Boot registers its own listeners, so you can remove the `<listeners>` block. See the [Configuration Reference](/docs/configuration) for every option.

---

## Migrating incrementally

You don't have to convert everything at once:

1. Add the dependency and a `selenium-boot.yml`.
2. Point **one** test class at `BaseTest`, delete its `@BeforeMethod`/`@AfterMethod`, and run it.
3. Once green, delete your `DriverFactory`, `WaitUtils`, retry analyzer, and screenshot listener as the last class stops referencing them.

Because Selenium Boot *is* Selenium, a half-migrated suite runs fine.

---

## Next steps

- [Getting Started](/docs/getting-started) — the 5-minute version
- [BaseTest](/docs/guides/base-test) / [BasePage](/docs/guides/base-page) — the base classes you'll extend
- [Accessibility-First Locators](/docs/guides/semantic-locators) — `getByRole`/`getByLabel`, once the boilerplate is gone
- [Configuration Reference](/docs/configuration) — the full `selenium-boot.yml`
