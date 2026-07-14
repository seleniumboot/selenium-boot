---
description: "Handle Shadow DOM in Selenium: Selenium Boot pierces shadow roots with shadowFind, shadowClick, and shadowType, and traverses nested web components with shadowPierce — no manual JavaScript."
id: handle-shadow-dom
title: Handle Shadow DOM
sidebar_label: Handle Shadow DOM
---

# Handle Shadow DOM

Elements inside a web component's **shadow root** aren't reachable with ordinary CSS from the page. `BasePage` gives you helpers that pierce the shadow boundary for you — no hand-written JavaScript.

```java title="SettingsPage.java"
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;

public class SettingsPage extends BasePage {

    public void updateEmail(String email) {
        // Host element <my-form>, then a selector scoped to its shadow root:
        shadowType(By.cssSelector("my-form"), "#email", email);
        shadowClick(By.cssSelector("my-form"), "#save");
    }

    public String bannerText() {
        return shadowGetText(By.cssSelector("my-banner"), ".message");
    }
}
```

**Nested** shadow roots (a component inside a component) — traverse them with `shadowPierce(...)`, passing the host selector at each level:

```java
// <checkout-flow> → shadow → <payment-widget> → shadow → #pay-btn
WebElement payBtn = shadowPierce("checkout-flow", "payment-widget", "#pay-btn");
payBtn.click();
```

Guard optional shadow content without a `try/catch` — `shadowExists(...)` never throws:

```java
if (shadowExists(By.cssSelector("my-form"), ".error")) {
    // handle validation error
}
```

:::caution CSS only
Shadow-root selectors must be **CSS** — XPath cannot cross a shadow boundary.
:::

**Deeper reference:** [BasePage](/docs/guides/base-page) — page-object base class and its Shadow DOM helpers.
