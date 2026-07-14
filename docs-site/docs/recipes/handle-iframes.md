---
description: "Handle iframes in Selenium without manual switchTo() bookkeeping: Selenium Boot's withinFrame runs your actions inside the frame and restores the previous context automatically, even when nested."
id: handle-iframes
title: Handle iframes
sidebar_label: Handle iframes
---

# Handle iframes

Interacting with content inside an `<iframe>` normally means `driver.switchTo().frame(...)` and remembering to switch back. `BasePage`'s `withinFrame(...)` does the bookkeeping for you: it switches in, runs your action, and restores the previous context — even when nested.

```java title="CheckoutPage.java"
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;

public class CheckoutPage extends BasePage {

    public void payWithCard(String number) {
        withinFrame(By.id("payment-iframe"), () -> {
            type(By.id("card-number"), number);
            click(By.id("pay"));
        });
        // Back in the main document automatically here.
    }
}
```

Nesting is safe — inner frames restore to their **parent** frame, not the top document:

```java
withinFrame(By.id("outer-iframe"), () -> {
    withinFrame(By.id("inner-iframe"), () -> {
        click(By.id("confirm"));
    });
    click(By.id("next"));   // still inside outer-iframe
});
```

Prefer to target a frame by index or by its `name`/`id` attribute? Use `withinFrameIndex(int, ...)` or `withinFrameName(String, ...)`.

**Deeper reference:** [BasePage](/docs/guides/base-page) — page-object base class and its frame helpers.
