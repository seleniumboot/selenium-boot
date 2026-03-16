---
id: browser-lifecycle
title: Browser Lifecycle
sidebar_position: 7
---

# Browser Lifecycle

Control when the WebDriver session is created and closed.

---

## Configuration

```yaml title="selenium-boot.yml"
browser:
  lifecycle: per-test   # per-test (default) | per-suite
```

---

## `per-test` (default)

A fresh browser opens before every test method and closes immediately after.

```
Test 1 starts → Chrome opens → test runs → Chrome closes
Test 2 starts → Chrome opens → test runs → Chrome closes
Test 3 starts → Chrome opens → test runs → Chrome closes
```

**Best for:** Independent tests, full isolation, CI pipelines.

---

## `per-suite`

The browser opens once per thread and stays alive for the entire suite. It is closed cleanly when the suite finishes.

```
Suite starts
  Thread 1: Chrome opens
    Test 1 runs → browser stays open
    Test 2 runs → browser stays open
    Test 3 runs → browser stays open
  Thread 1: Chrome closes
Suite ends
```

**Best for:** Large sequential suites where browser startup time is a bottleneck, or test flows where tests share an authenticated session.

---

## Parallel + per-suite

In parallel execution, each thread manages its own browser independently.

```
Thread 1: Chrome opens → runs Test A, B, C → Chrome closes
Thread 2: Chrome opens → runs Test D, E, F → Chrome closes
```

The semaphore still limits the maximum number of concurrent browsers (`maxActiveSessions`).

---

## Managing state between tests

With `per-suite`, the browser retains the full state (cookies, URL, localStorage) from the previous test.

**Reset state explicitly** if your tests are independent:

```java
@Test
public void independentTest() {
    open();  // navigate to baseUrl — resets the page
    getDriver().manage().deleteAllCookies();  // clear session if needed
    // ...
}
```

**Rely on state intentionally** for dependent flows:

```java
@Test(priority = 1)
public void login() {
    open("/login");
    new LoginPage(getDriver()).login("admin", "secret");
    // browser now has an authenticated session
}

@Test(priority = 2, dependsOnMethods = "login")
public void viewDashboard() {
    // no login needed — session cookie is still in the browser
    open("/dashboard");
    Assert.assertTrue(new DashboardPage(getDriver()).isLoaded());
}

@Test(priority = 3, dependsOnMethods = "viewDashboard")
public void editProfile() {
    open("/profile");
    // still authenticated
}
```

---

## Screenshots on failure

Screenshots still work correctly with `per-suite` — the browser is alive when the failure screenshot is taken.

---

## Retry with `per-suite`

When a test is retried, the same browser instance is reused. No restart occurs. If the previous attempt left the browser in a bad state, call `open()` at the start of your test to navigate back to a known page.
