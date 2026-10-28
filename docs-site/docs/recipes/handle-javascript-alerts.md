---
description: "Handle JavaScript alert, confirm and prompt dialogs in Selenium with BasePage helpers that wait for the dialog and read, accept, dismiss or type into it."
id: handle-javascript-alerts
title: Handle JavaScript alerts
sidebar_label: Handle JavaScript alerts
---

# Handle JavaScript alerts

Native `alert()`, `confirm()` and `prompt()` dialogs block the page until you answer them. `BasePage` gives you one-line helpers for each case. They all **wait** for the dialog to appear (up to `timeouts.explicit`), so you don't need your own `WebDriverWait` or `switchTo().alert()`.

```java title="SettingsPage.java"
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SettingsPage extends BasePage {

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    // alert(): read the message, then click OK
    public String saveAndReadMessage() {
        click(By.id("save"));
        return getAndAcceptAlert();          // text + accept in one step
    }

    // alert(): read first, decide later
    public void saveAndCheck() {
        click(By.id("save"));
        String text = getAlertText();        // dialog stays open
        acceptAlert();                       // OK
    }

    // confirm(): OK or Cancel
    public void deleteAccount(boolean really) {
        click(By.id("delete"));
        if (really) acceptAlert(); else dismissAlert();
    }

    // prompt(): type a value and click OK
    public void rename(String name) {
        click(By.id("rename"));
        typeInAlert(name);
    }
}
```

| Helper | What it does |
|---|---|
| `acceptAlert()` | Clicks OK |
| `dismissAlert()` | Clicks Cancel |
| `getAlertText()` | Returns the message and leaves the dialog open |
| `getAndAcceptAlert()` | Returns the message, then clicks OK |
| `typeInAlert(String)` | Types into a `prompt()` and clicks OK |

## Why `unhandledPromptBehavior: ignore`

The W3C default is *dismiss and notify*: the browser closes an open dialog before your next WebDriver command runs, so by the time you call `switchTo().alert()` it's gone and you get `NoAlertPresentException`. Selenium Boot sets `unhandledPromptBehavior` to `ignore` on its local Chrome, Firefox and remote drivers, so the dialog stays open until you handle it.

If you build a driver yourself (a custom provider), set the same capability:

```java
options.setCapability("unhandledPromptBehavior", "ignore");
```

**Deeper reference:** [BasePage](/docs/guides/base-page): the page-object base class these helpers live on.
