---
id: base-page
title: BasePage
sidebar_position: 2
---

# BasePage

`BasePage` provides ready-to-use helper methods for page objects so you never write raw Selenium calls in your tests.

---

## Creating a page object

```java
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT   = By.id("submit");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(SUBMIT);
    }
}
```

---

## Available methods

### `click(By locator)`
Waits for the element to be clickable, then clicks it.

```java
click(By.id("submit"));
```

### `type(By locator, String text)`
Clears the field, then types the given text. Waits for visibility first.

```java
type(By.id("username"), "admin");
```

### `getText(By locator)`
Waits for the element to be visible, returns its text.

```java
String heading = getText(By.cssSelector("h1"));
```

### `getAttribute(By locator, String attribute)`
Waits for the element to be visible, returns the given attribute value.

```java
String value = getAttribute(By.id("input"), "value");
String href  = getAttribute(By.cssSelector("a.link"), "href");
```

### `isDisplayed(By locator)`
Returns `true` if the element is present and visible, `false` otherwise. Does not throw.

```java
if (isDisplayed(By.id("error-banner"))) {
    // handle error
}
```

---

## Using WaitEngine directly

For advanced waits not covered by the helpers above, use `getWait()`:

```java
import com.seleniumboot.wait.WaitEngine;

public class DashboardPage extends BasePage {

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        getWait().waitForInvisible(By.cssSelector(".spinner"));
        return isDisplayed(By.id("dashboard-content"));
    }
}
```

---

## Full page object example

```java
public class CheckoutPage extends BasePage {

    private static final By QUANTITY  = By.name("qty");
    private static final By PAY_BTN   = By.id("pay-now");
    private static final By SUCCESS   = By.cssSelector(".order-success");

    public CheckoutPage(WebDriver driver) { super(driver); }

    public void setQuantity(int qty) {
        type(QUANTITY, String.valueOf(qty));
    }

    public void pay() {
        click(PAY_BTN);
        getWait().waitForVisible(SUCCESS);
    }

    public String getConfirmationMessage() {
        return getText(SUCCESS);
    }
}
```
