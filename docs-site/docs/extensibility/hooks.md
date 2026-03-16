---
id: hooks
title: Hooks
sidebar_position: 2
---

# Hooks

Selenium Boot exposes TestNG lifecycle hooks through `BaseTest`. Use them to run setup and teardown logic at the suite, class, or method level.

---

## Available hooks

| Method | When it runs |
|---|---|
| `@BeforeSuite` | Once before any test in the suite |
| `@AfterSuite` | Once after all tests in the suite |
| `@BeforeClass` | Once before the first test in a class |
| `@AfterClass` | Once after the last test in a class |
| `@BeforeMethod` | Before each test method |
| `@AfterMethod` | After each test method (passes/fails/skips) |

---

## Suite-level hooks

```java
public class BaseSetup extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void globalSetup() {
        // runs once — connect to test database, set up test data, etc.
    }

    @AfterSuite(alwaysRun = true)
    public void globalTeardown() {
        // runs once — clean up test data, close connections
    }
}
```

Have all test classes extend `BaseSetup` instead of `BaseTest` directly.

---

## Class-level hooks

```java
public class LoginTest extends BaseTest {

    @BeforeClass
    public void setupLoginTestData() {
        // create test users via API before any LoginTest method runs
    }

    @AfterClass
    public void cleanupLoginTestData() {
        // delete test users after all LoginTest methods complete
    }
}
```

---

## Method-level hooks

```java
public class CheckoutTest extends BaseTest {

    @BeforeMethod
    public void navigateToStart() {
        open("/");   // always start from the home page
    }

    @AfterMethod
    public void clearCart() {
        // reset cart state via API if test left it dirty
    }
}
```

---

## Accessing the driver in hooks

`getDriver()` is available in any hook — the driver is created before `@BeforeMethod` and destroyed after `@AfterMethod` (in `per-test` lifecycle mode).

```java
@BeforeMethod
public void setup() {
    open("/login");
    new LoginPage(getDriver()).login("admin", "secret");
}
```

---

## `@AfterMethod` with `ITestResult`

TestNG passes the test result to `@AfterMethod` if you declare it as a parameter:

```java
@AfterMethod
public void afterEach(ITestResult result) {
    if (!result.isSuccess()) {
        // extra cleanup only on failure
        getDriver().manage().deleteAllCookies();
    }
}
```

---

## Hook ordering

When multiple classes define the same hook type and form an inheritance chain, TestNG calls them in the expected order:

```
@BeforeSuite  (grandparent → parent → child)
@BeforeClass  (grandparent → parent → child)
@BeforeMethod (grandparent → parent → child)
  [test runs]
@AfterMethod  (child → parent → grandparent)
@AfterClass   (child → parent → grandparent)
@AfterSuite   (child → parent → grandparent)
```
