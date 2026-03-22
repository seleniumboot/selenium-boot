---
id: getting-started
title: Getting Started
sidebar_position: 2
---

# Getting Started

Get your first Selenium Boot test running in under 5 minutes.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Chrome or Firefox installed

:::info
No WebDriver binaries required — Selenium Manager handles browser driver downloads automatically.
:::

---

## Step 1 — Add the dependency

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>0.10.0</version>
</dependency>
```

---

## Step 2 — Create the configuration file

Create `selenium-boot.yml` in your project root (next to `pom.xml`):

```yaml title="selenium-boot.yml"
browser:
  name: chrome
  headless: false

execution:
  baseUrl: https://your-app.com

retry:
  enabled: true
  maxAttempts: 2

timeouts:
  explicit: 10
  pageLoad: 30
```

---

## Step 3 — Write your first test

```java title="src/test/java/com/example/LoginTest.java"
import com.seleniumboot.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(description = "Valid user can log in")
    public void loginTest() {
        open();  // navigates to baseUrl
        // your test steps here
        Assert.assertTrue(getDriver().getTitle().contains("Dashboard"));
    }
}
```

---

## Step 4 — Create a TestNG suite

```xml title="testng.xml"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="selenium-boot-suite" verbose="1">
    <test name="MyTests">
        <classes>
            <class name="com.example.LoginTest"/>
        </classes>
    </test>
</suite>
```

---

## Step 5 — Run

```bash
mvn test
```

---

## What happens

1. Framework loads `selenium-boot.yml`
2. Chrome launches automatically
3. Your test runs
4. Screenshot captured on any failure
5. Browser closes
6. HTML report generated at `target/selenium-boot-report.html`
7. Metrics JSON at `target/selenium-boot-metrics.json`

---

## Project structure

```
your-project/
├── pom.xml
├── selenium-boot.yml          ← framework config
├── testng.xml                 ← test suite definition
└── src/
    └── test/
        └── java/
            └── com/example/
                ├── pages/
                │   └── LoginPage.java
                └── tests/
                    └── LoginTest.java
```

---

## Working example project

A complete working project is available at:
**https://github.com/seleniumboot/selenium-boot-test**

Clone it, run `mvn test`, and you'll have a full working suite with page objects, step logging, and retry configured.

---

## Next steps

- [Configuration Reference](/docs/configuration) — all available config options
- [BasePage](/docs/guides/base-page) — write clean page objects
- [Step Logging](/docs/guides/step-logging) — add named steps to your tests
