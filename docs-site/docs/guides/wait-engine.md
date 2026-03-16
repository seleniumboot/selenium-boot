---
id: wait-engine
title: WaitEngine
sidebar_position: 3
---

# WaitEngine

`WaitEngine` provides fluent explicit waits. It is pre-configured with the timeout from `selenium-boot.yml` (`timeouts.explicit`) and is available in every `BasePage` via `getWait()`.

---

## Available methods

### Element visibility

```java
getWait().waitForVisible(By.id("modal"));
getWait().waitForInvisible(By.cssSelector(".spinner"));  // wait for loaders to disappear
```

### Clickability

```java
getWait().waitForClickable(By.id("submit"));
```

### Text content

```java
getWait().waitForText(By.cssSelector("h1"), "Welcome back");
```

### Attribute value

```java
getWait().waitForAttributeContains(By.id("status"), "class", "active");
```

### DOM staleness

```java
WebElement old = driver.findElement(By.id("row-1"));
getWait().waitForStaleness(old);  // wait for DOM replacement / AJAX reload
```

### Page load

```java
getWait().waitForPageLoad();  // waits until document.readyState === "complete"
```

### Custom condition

```java
// Escape hatch — pass any ExpectedCondition
getWait().wait(ExpectedConditions.numberOfWindowsToBe(2));
```

---

## Timeout override

Use a custom timeout for a single wait without changing the global config:

```java
getWait(30).waitForVisible(By.id("slow-element"));  // 30-second timeout
```

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
getWait().waitForVisible(By.id("result"));
```

```java
// ❌ raw WebDriverWait — bypasses framework timeout config
new WebDriverWait(driver, Duration.ofSeconds(10))
    .until(ExpectedConditions.visibilityOf(...));

// ✅ use getWait() — reads timeout from config
getWait().waitForVisible(By.id("result"));
```
