---
id: hooks
title: Execution Hooks
sidebar_position: 2
---

# Execution Hooks

`ExecutionHook` lets you inject behaviour at key points in the test execution lifecycle — suite start/end and per-test start/end/failure. All methods are optional (default no-op).

---

## Create a hook

```java
import com.seleniumboot.hooks.ExecutionHook;

public class TimingHook implements ExecutionHook {

    @Override
    public void onSuiteStart() {
        // called once after framework bootstrap, before any test runs
    }

    @Override
    public void onSuiteEnd() {
        // called once after all reports are generated
    }

    @Override
    public void onTestStart(String testId) {
        // testId = "com.example.LoginTest#loginTest"
        System.out.println("Starting: " + testId);
    }

    @Override
    public void onTestEnd(String testId, String status) {
        // status = "PASSED" or "SKIPPED"
        metricsClient.record(testId, status);
    }

    @Override
    public void onTestFailure(String testId, Throwable cause) {
        // called after screenshot is captured, before driver is quit
        alertService.send("FAILED: " + testId + " — " + cause.getMessage());
    }
}
```

Only override the methods you need — the others are no-ops by default.

---

## Register via Java SPI (auto-discovery)

```
src/main/resources/META-INF/services/com.seleniumboot.hooks.ExecutionHook
```

Contents:

```
com.example.hooks.TimingHook
```

---

## Register programmatically

```java
import com.seleniumboot.hooks.HookRegistry;

HookRegistry.register(new TimingHook());
```

Call this before the suite starts (e.g. in a `@BeforeSuite` method or a `SeleniumBootPlugin.onLoad`).

---

## Hook event order

```
onSuiteStart()
  onTestStart("LoginTest#login")
  onTestEnd("LoginTest#login", "PASSED")

  onTestStart("CheckoutTest#checkout")
  onTestFailure("CheckoutTest#checkout", AssertionError)
  // note: onTestEnd is NOT called when onTestFailure fires

onSuiteEnd()
```

---

## Error isolation

Hook failures are **isolated** — an exception in one hook is logged but does not prevent other hooks from running or affect the test results.

---

## Hook vs Plugin

| | `ExecutionHook` | `SeleniumBootPlugin` |
|---|---|---|
| Per-test events | Yes | No |
| Suite events | Yes | Yes (onLoad/onUnload) |
| Access to config | No | Yes (via onLoad) |
| Typical use | Per-test metrics, alerts | Initialisation, external clients |
