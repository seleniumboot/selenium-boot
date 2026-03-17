---
id: console-errors
title: Console Error Collector
sidebar_position: 11
---

# Console Error Collector

`ConsoleErrorCollector` captures JavaScript console errors during test execution. Use it to catch silent JS failures that don't cause visible test failures.

---

## Configuration

```yaml title="selenium-boot.yml"
browser:
  captureConsoleErrors: true    # collect JS errors (default: false)
  failOnConsoleErrors: false    # fail test on any JS error (default: false)
```

---

## Manual collection in a test

```java
import com.seleniumboot.browser.ConsoleErrorCollector;

@Test
public void noJsErrorsOnLogin() {
    open("/login");
    ConsoleErrorCollector.injectShim();   // start capturing

    type(By.id("username"), "admin");
    type(By.id("password"), "secret");
    click(By.id("submit"));

    List<String> errors = ConsoleErrorCollector.getErrors();
    Assert.assertTrue(errors.isEmpty(), "Unexpected JS errors: " + errors);
}
```

---

## Clear between interactions

Isolate which action produced errors:

```java
open("/checkout");
ConsoleErrorCollector.injectShim();

click(By.id("add-to-cart"));
List<String> addErrors = ConsoleErrorCollector.getErrors();

ConsoleErrorCollector.clear();   // reset before next action

click(By.id("proceed-to-payment"));
List<String> paymentErrors = ConsoleErrorCollector.getErrors();
```

---

## Chrome vs Firefox

| Browser | Method | Notes |
|---|---|---|
| Chrome | WebDriver browser logs | Works automatically, no shim needed |
| Firefox | JS console shim | Call `injectShim()` after page load |

Chrome collects errors automatically via `LogType.BROWSER`. Firefox requires the JS shim because it does not expose browser logs through WebDriver.

---

## API

### `ConsoleErrorCollector.injectShim()`
Injects a JavaScript shim that intercepts `console.error` calls. Call after page load on Firefox or for cross-browser consistency.

### `ConsoleErrorCollector.collect()`
Returns all JS errors captured since the last `clear()`. Reads from WebDriver logs (Chrome) or the JS shim buffer.

### `ConsoleErrorCollector.getErrors()`
Alias for `collect()`.

### `ConsoleErrorCollector.clear()`
Resets the JS shim error buffer on the current page.

### `ConsoleErrorCollector.isEnabled()`
Returns `true` if `browser.captureConsoleErrors: true` is set in config.
