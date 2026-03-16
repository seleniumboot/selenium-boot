---
id: parallel
title: Parallel Execution
sidebar_position: 5
---

# Parallel Execution

Selenium Boot supports parallel test execution out of the box. Configure the thread count in `selenium-boot.yml` and the framework handles thread-safe driver management automatically.

---

## Configuration

```yaml title="selenium-boot.yml"
parallel:
  enabled: true
  threadCount: 4     # number of concurrent browser sessions

browser:
  maxActiveSessions: 4   # semaphore cap — cannot exceed threadCount
```

`maxActiveSessions` acts as a hard ceiling on concurrent browsers. If `threadCount` is 4 but `maxActiveSessions` is 2, at most 2 browsers will run at the same time.

---

## TestNG suite file

Parallel execution requires a TestNG suite XML file:

```xml title="testng.xml"
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="SeleniumBoot" parallel="methods" thread-count="4">
    <test name="All Tests">
        <classes>
            <class name="com.example.tests.LoginTest"/>
            <class name="com.example.tests.CheckoutTest"/>
            <class name="com.example.tests.SearchTest"/>
        </classes>
    </test>
</suite>
```

Run it with Maven Surefire:

```xml title="pom.xml"
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <suiteXmlFiles>
            <suiteXmlFile>testng.xml</suiteXmlFile>
        </suiteXmlFiles>
    </configuration>
</plugin>
```

---

## How thread safety works

| Concern | How Selenium Boot handles it |
|---|---|
| WebDriver instance | `ThreadLocal<WebDriver>` — each thread has its own driver |
| Config access | `AtomicReference` — reads are lock-free |
| Step recording | `ConcurrentHashMap` keyed by test ID |
| Session limiting | `Semaphore` (fair) — acquired before driver create, released after quit |
| Screenshot capture | Reads driver from `ThreadLocal` — always the right instance |

You do not need to do anything special in your tests. `getDriver()` always returns the driver for the calling thread.

---

## Parallel modes

TestNG supports several parallel modes. Selenium Boot works with all of them:

| Mode | Description | Recommended |
|---|---|---|
| `methods` | Each test method runs in its own thread | Best general choice |
| `classes` | Each test class runs in a thread | Use when tests within a class must be sequential |
| `tests` | Each `<test>` tag in suite XML runs in a thread | For suite-level isolation |

---

## Parallel + per-suite lifecycle

When `browser.lifecycle: per-suite` is combined with parallel execution, each thread opens its own browser once and reuses it for all tests on that thread.

```
Thread 1: Chrome opens → Test A → Test B → Test C → Chrome closes
Thread 2: Chrome opens → Test D → Test E → Test F → Chrome closes
```

`maxActiveSessions` still limits the total concurrent browsers.

---

## Writing parallel-safe tests

**Do not use static mutable state** — static fields are shared across threads:

```java
// ❌ not thread-safe
public class LoginTest extends BaseTest {
    private static LoginPage loginPage;   // shared — threads overwrite each other

    @Test
    public void login() {
        loginPage = new LoginPage(getDriver());
        loginPage.login("admin", "secret");
    }
}
```

```java
// ✅ thread-safe — instance field, each thread has its own test instance
public class LoginTest extends BaseTest {
    private LoginPage loginPage;

    @Test
    public void login() {
        loginPage = new LoginPage(getDriver());
        loginPage.login("admin", "secret");
    }
}
```

TestNG creates a new instance of each test class per thread, so instance fields are safe.

---

## Disabling parallel execution

```yaml
parallel:
  enabled: false
```

Or simply omit the `parallel` block — sequential execution is the default.
