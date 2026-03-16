---
id: base-test
title: BaseTest
sidebar_position: 1
---

# BaseTest

`BaseTest` is the mandatory superclass for all Selenium Boot tests. Extending it is the only setup required.

---

## Usage

```java
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    public void loginTest() {
        open();  // navigate to baseUrl
        // test steps
    }
}
```

---

## What BaseTest does

- Registers `SuiteExecutionListener` (framework bootstrap, config loading, report generation)
- Registers `TestExecutionListener` (driver creation, screenshot on failure, metrics recording)
- Provides helper methods so tests never interact with `WebDriver` or config directly

---

## Available methods

### `open()`
Navigates the browser to the `baseUrl` configured in `selenium-boot.yml`.

```java
open();  // → browser.get(config.execution.baseUrl)
```

### `open(String path)`
Navigates to `baseUrl + path`.

```java
open("/login");    // → browser.get("https://your-app.com/login")
open("/admin");    // → browser.get("https://your-app.com/admin")
```

### `getDriver()`
Returns the `WebDriver` instance bound to the current thread.

```java
WebDriver driver = getDriver();
```

:::caution
Never call `driver.quit()` or `new ChromeDriver()` in your test code.
The framework manages the driver lifecycle — creating it before each test and quitting it after.
:::

---

## Rules

| Rule | Reason |
|---|---|
| Do NOT create `WebDriver` manually | Framework manages lifecycle |
| Do NOT call `driver.quit()` | Causes session errors in subsequent steps |
| Do NOT use `Thread.sleep()` | Use `WaitEngine` instead |
| Do NOT manage retry in `@AfterMethod` | Framework handles retry automatically |
