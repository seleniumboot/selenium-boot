---
id: step-logging
title: Step Logging
sidebar_position: 6
---

# Step Logging

`StepLogger` lets you log named steps during test execution. Steps appear as a timeline in the HTML report inside each test's detail panel.

---

## Basic usage

```java
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;

public class LoginTest extends BaseTest {

    @Test(description = "Valid user can log in")
    public void loginTest() {
        StepLogger.step("Open login page");
        open();

        StepLogger.step("Enter credentials");
        loginPage.login("admin", "secret");

        StepLogger.step("Assert dashboard visible", StepStatus.PASS);
        Assert.assertTrue(dashboardPage.isLoaded());
    }
}
```

---

## API

### `StepLogger.step(String name)`
Logs a step with `INFO` status and no screenshot.

```java
StepLogger.step("Navigate to login page");
```

### `StepLogger.step(String name, boolean screenshot)`
Logs a step with `INFO` status. When `true`, captures an inline screenshot.

```java
StepLogger.step("After form submission", true);  // screenshot captured here
```

### `StepLogger.step(String name, StepStatus status)`
Logs a step with an explicit status — `INFO`, `PASS`, or `FAIL`.

```java
StepLogger.step("Verify order total", StepStatus.PASS);
StepLogger.step("Payment rejected", StepStatus.FAIL);
```

### `StepLogger.step(String name, StepStatus status, boolean screenshot)`
Combines explicit status with optional screenshot.

```java
StepLogger.step("Assert confirmation page", StepStatus.PASS, true);
```

---

## Step statuses

| Status | When to use |
|---|---|
| `INFO` | Default — neutral step, no outcome implied |
| `PASS` | Explicitly marking a verification as passed |
| `FAIL` | Explicitly marking a step as failed (test continues) |

---

## In the HTML report

Steps appear in the **detail panel** when you expand a test row (Test Cases tab) or in the Failures tab where detail panels are pre-opened.

Each step shows:
- Step number
- Step name
- Time offset from test start (e.g. `+312ms`)
- Status badge (INFO / PASS / FAIL)
- Thumbnail if a screenshot was captured (click to open full-size lightbox)

---

## Thread safety

`StepLogger` uses `SeleniumBootContext.getCurrentTestId()` to bind steps to the correct test. It is **thread-safe** — safe to use in parallel test execution. Each thread's steps are recorded independently.

---

## Retry behaviour

When a test is retried, steps from the previous attempt are **cleared automatically** before the retry begins. The report shows only the final attempt's steps.

---

## Example — full test with steps

```java
@Test(description = "Complete checkout flow")
public void checkoutTest() {
    StepLogger.step("Open home page");
    open();

    StepLogger.step("Search for product");
    homePage.search("wireless headphones");

    StepLogger.step("Select first result", true);  // screenshot of results
    searchPage.selectFirstResult();

    StepLogger.step("Add to cart");
    productPage.addToCart();

    StepLogger.step("Proceed to checkout", true);  // screenshot of cart
    cartPage.checkout();

    StepLogger.step("Enter payment details");
    checkoutPage.fillPayment("4111111111111111", "12/28", "123");

    StepLogger.step("Place order");
    checkoutPage.placeOrder();

    StepLogger.step("Verify confirmation", StepStatus.PASS, true);  // screenshot of confirmation
    Assert.assertTrue(confirmationPage.isSuccess());
}
```
