---
id: smart-locator
title: SmartLocator
sidebar_position: 9
---

# SmartLocator

`SmartLocator` tries multiple locator strategies in order and returns the first element that is found and visible. Use it when an element's locator may differ across environments, browsers, or application versions.

---

## Basic usage

```java
import com.seleniumboot.test.SmartLocator;

// Try CSS first, fall back to XPath
WebElement btn = SmartLocator.find(driver,
    By.cssSelector(".submit-btn"),
    By.xpath("//button[@type='submit']"),
    By.id("submit")
);
```

---

## From inside a page object

```java
public class LoginPage extends BasePage {

    public void clickSubmit() {
        // resilient — works if any of these locators matches
        WebElement btn = SmartLocator.find(driver,
            By.id("submit"),
            By.cssSelector("button[type='submit']"),
            By.xpath("//button[contains(text(),'Log in')]")
        );
        btn.click();
    }
}
```

---

## Check visibility without throwing

```java
boolean anyVisible = SmartLocator.isAnyVisible(driver,
    By.id("error-banner"),
    By.cssSelector(".alert-error")
);

if (anyVisible) {
    // handle error state
}
```

---

## When to use SmartLocator

| Situation | Use SmartLocator? |
|---|---|
| Element locator changes between environments | Yes |
| Multiple browsers with slightly different DOM | Yes |
| App under active development with unstable selectors | Yes |
| Stable, well-maintained locators | No — use `By` directly |

---

## Logging

When `SmartLocator` resolves an element, it logs which strategy succeeded:

```
[SmartLocator] Resolved using: By.cssSelector: .submit-btn
```

This makes it easy to identify which locator is actually being used in CI logs.
