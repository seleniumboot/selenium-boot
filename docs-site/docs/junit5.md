---
id: junit5
title: JUnit 5 Support
sidebar_position: 10
---

# JUnit 5 Support

Selenium Boot supports both **TestNG** (built-in) and **JUnit 5** (opt-in). The JUnit 5 integration provides the same lifecycle — driver management, HTML report, step timeline, screenshots, AI failure analysis, tracing, and flakiness tracking — with zero configuration beyond adding the dependency.

---

## Setup

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>1.10.0</version>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-launcher</artifactId>
    <version>1.10.2</version>
    <scope>test</scope>
</dependency>
```

No extra Surefire configuration needed — Maven Surefire 3.x auto-detects JUnit 5.

---

## Option A — Extend `BaseJUnit5Test`

The simplest approach. Identical to extending `BaseTest` in TestNG:

```java
class LoginTest extends BaseJUnit5Test {

    @Test
    void validLogin() {
        open();
        step("Enter credentials");
        $("input#username").type("admin");
        $("input#password").type("secret");
        $("button[type='submit']").click();

        step("Verify dashboard", true);
        assertThat(By.id("dashboard")).isVisible();
    }
}
```

`BaseJUnit5Test` provides: `getDriver()`, `getWait()`, `open()`, `open(path)`, `$()`, `assertThat()`, `step()`.

---

## Option B — `@EnableSeleniumBoot`

Composed annotation for your own base class:

```java
@EnableSeleniumBoot
abstract class AppTest {
    protected WebDriver getDriver() { return DriverManager.getDriver(); }
}
```

---

## Option C — Parameter injection

No base class needed. Inject `WebDriver` directly into test methods:

```java
@ExtendWith(SeleniumBootExtension.class)
class LoginTest {

    @Test
    void validLogin(WebDriver driver) {
        driver.get("https://example.com/login");
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }
}
```

---

## Parallel execution

```properties title="src/test/resources/junit-platform.properties"
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

ThreadLocal driver isolation makes all three options thread-safe.

---

## Feature parity with TestNG

| Feature | Status |
|---|---|
| Auto driver provisioning | ✅ |
| `selenium-boot.yml` config | ✅ |
| Parallel execution | ✅ |
| `WaitEngine` | ✅ |
| `BasePage` | ✅ |
| `StepLogger` / `step()` | ✅ |
| HTML report + step timeline | ✅ |
| Screenshot on failure | ✅ |
| AI failure analysis | ✅ |
| Trace viewer | ✅ |
| Self-healing locators | ✅ |
| Fluent Locator API `$()` | ✅ |
| Web-first assertions `assertThat()` | ✅ |
| Network mocking | ✅ |
| Visual regression | ✅ |
| Flakiness prediction | ✅ |
| Video recording | ✅ |
| JUnit XML output | Native |
| `@Retryable` retry | Planned |
| `@PreCondition` session cache | Planned |
