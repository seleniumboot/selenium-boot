---
id: junit5
title: JUnit 5 Support
sidebar_position: 10
---

# JUnit 5 Support

:::info Roadmap
JUnit 5 support is planned for a future release. The current version of Selenium Boot is built on **TestNG**. This page tracks what JUnit 5 support will look like when it ships.
:::

---

## Current status

Selenium Boot `0.7.x` supports **TestNG only**. The core framework (driver management, config loading, reporting, retry) is TestNG-specific.

---

## Planned JUnit 5 API

The JUnit 5 integration will use a JUnit Extension (`@ExtendWith`) to inject the WebDriver and framework services:

```java
// Planned API — not yet available
@ExtendWith(SeleniumBootExtension.class)
public class LoginTest {

    @Test
    void validLogin(WebDriver driver) {
        driver.get("https://example.com/login");
        // ...
    }
}
```

Or with constructor injection:

```java
@ExtendWith(SeleniumBootExtension.class)
public class LoginTest {

    private final WebDriver driver;

    LoginTest(WebDriver driver) {
        this.driver = driver;
    }

    @Test
    void validLogin() {
        driver.get("https://example.com/login");
    }
}
```

---

## What will carry over

All framework features are planned to be available under JUnit 5:

| Feature | Status |
|---|---|
| Auto driver provisioning | Planned |
| `selenium-boot.yml` config | Planned |
| Parallel execution | Planned |
| Retry (`@Retryable`) | Planned |
| `WaitEngine` | Planned |
| `BasePage` | Planned |
| `StepLogger` | Planned |
| HTML report | Planned |
| JUnit XML output | Native — JUnit 5 generates this already |

---

## Using TestNG today

Until JUnit 5 support ships, use TestNG. It provides everything JUnit 5 does for UI testing and integrates cleanly with Maven Surefire:

```xml title="pom.xml"
<dependency>
    <groupId>io.github.selenium-boot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>0.7.0</version>
</dependency>
```

No extra TestNG dependency is required — it is included transitively.

---

## Track this feature

Follow the GitHub repository for updates on JUnit 5 support.
