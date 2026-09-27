---
description: "Explicit waits in Selenium without Thread.sleep(): WaitEngine gives fluent, auto-configured waits driven by your selenium-boot.yml timeout."
id: wait-engine
title: Selenium Waits (WaitEngine)
sidebar_label: WaitEngine
sidebar_position: 3
---

# WaitEngine

`WaitEngine` provides explicit waits as static methods, pre-configured with the timeout from `selenium-boot.yml` (`timeouts.explicit`). Call them directly — `WaitEngine.waitForVisible(...)` — from anywhere in a `BasePage` or `BaseTest`.

```java
import com.seleniumboot.wait.WaitEngine;
```

---

## Available methods

### Element visibility

```java
WaitEngine.waitForVisible(By.id("modal"));
WaitEngine.waitForInvisible(By.cssSelector(".spinner"));  // wait for loaders to disappear
```

### Clickability

```java
WaitEngine.waitForClickable(By.id("submit"));
```

### Text content

```java
WaitEngine.waitForText(By.cssSelector("h1"), "Welcome back");
```

### Attribute value

```java
WaitEngine.waitForAttributeContains(By.id("status"), "class", "active");  // substring
WaitEngine.waitForAttribute(By.id("status"), "aria-expanded", "true");    // exact match
```

### Text matches (regex)

```java
// Wait until the element's visible text matches a regular expression
WaitEngine.waitForTextMatches(By.cssSelector(".total"), "\\$\\d+\\.\\d{2}");
```

### URL matches (regex)

```java
WaitEngine.waitForUrlContains("/orders");            // substring
WaitEngine.waitForUrlMatches(".*/orders/\\d+");      // regular expression
```

### DOM staleness

```java
WebElement old = driver.findElement(By.id("row-1"));
WaitEngine.waitForStaleness(old);  // wait for DOM replacement / AJAX reload
```

### Page load

```java
WaitEngine.waitForPageLoad();  // waits until document.readyState === "complete"
```

### Network

Waits driven by the browser's network activity — CDP on Chrome/Edge, [BiDi](https://www.w3.org/TR/webdriver-bidi/) on Firefox and other BiDi-capable browsers. Throws `UnsupportedOperationException` on drivers with neither.

```java
// Wait until no request has been in flight for 500ms (the default), or a custom quiet period
WaitEngine.waitForNetworkIdle();
WaitEngine.waitForNetworkIdle(Duration.ofSeconds(1));

// Wait for a response matching a URL glob (same syntax as NetworkMock.stub), get its status code
int status = WaitEngine.waitForResponse("**/api/checkout");
```

Both are bounded by the configured explicit timeout by default; pass a `Duration` to override it for `waitForResponse`.

### Custom condition

```java
// Escape hatch — pass any ExpectedCondition
WaitEngine.wait(ExpectedConditions.numberOfWindowsToBe(2));
```

---

## Timeout override

`waitForNetworkIdle(Duration)` and `waitForResponse(String, Duration)` take an explicit timeout per call. Every other method reads `timeouts.explicit` from `selenium-boot.yml`.

---

## Configuration

```yaml title="selenium-boot.yml"
timeouts:
  explicit: 10   # seconds — default for all WaitEngine calls
  pageLoad: 30   # seconds — browser page load timeout
```

---

## Anti-patterns to avoid

```java
// ❌ never do this
Thread.sleep(3000);

// ✅ do this instead
WaitEngine.waitForVisible(By.id("result"));
```

```java
// ❌ raw WebDriverWait — bypasses framework timeout config
new WebDriverWait(driver, Duration.ofSeconds(10))
    .until(ExpectedConditions.visibilityOf(...));

// ✅ WaitEngine reads the timeout from config
WaitEngine.waitForVisible(By.id("result"));
```
