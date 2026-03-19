# Selenium Boot

**An Opinionated, Spring Boot–Inspired Framework for Java QA Automation**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.seleniumboot/selenium-boot)](https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**[Documentation](https://seleniumboot.github.io/selenium-boot) · [Sample Project](https://github.com/seleniumboot/selenium-boot-test) · [Changelog](#project-status)**

---

## Overview

Selenium Boot is an opinionated, production-ready automation framework for Java Selenium, inspired by the philosophy of Spring Boot.

It eliminates repetitive boilerplate by providing sensible defaults, a standardized project structure, and a convention-over-configuration approach — while keeping Selenium fully visible and accessible.

---

## What You Get Out of the Box

- Automatic WebDriver lifecycle management (no setup/teardown boilerplate)
- YAML-based configuration with environment profile switching
- Parallel execution with thread-safe driver isolation
- Smart explicit waits via `WaitEngine` — no more `Thread.sleep()`
- Automatic retry for flaky tests via `@Retryable`
- Screenshot capture on failure, embedded in report
- Advanced HTML report — pass rate gauge, donut chart, slowest tests, step timeline, dark mode
- `BasePage` — wait-backed `click`, `type`, `getText`, `isDisplayed`, iFrame helpers, file upload
- `SmartLocator` — tries multiple `By` strategies in order, returns first visible element
- `@PreCondition` — session-aware pre-conditions with automatic cookie + localStorage caching
- `ConsoleErrorCollector` — capture JS console errors (Chrome via logs, Firefox via shim)
- `DownloadManager` — poll download directory, handle partial files
- `StepLogger` — named test steps with timestamps and per-step screenshots
- Plugin system — custom browser providers, report adapters, lifecycle hooks via Java SPI
- CI-ready — auto-detects GitHub Actions, Jenkins, CircleCI; forces headless, emits JUnit XML

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Chrome or Firefox installed

No WebDriver binaries required — Selenium Manager handles it automatically.

---

### Step 1: Add the Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>0.9.1</version>
</dependency>
```

Also add the Surefire plugin so `mvn test` discovers TestNG tests:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

---

### Step 2: Create the Configuration File

Create `selenium-boot.yml` at your **project root** (same level as `pom.xml`):

```yaml
execution:
  mode: local           # local | remote
  baseUrl: https://example.com
  parallel: methods     # none | methods | classes
  threadCount: 4
  maxActiveSessions: 4

browser:
  name: chrome          # chrome | firefox
  headless: false
  lifecycle: per-test   # per-test (default) | per-suite
  captureConsoleErrors: true
  arguments:
    - --start-maximized
    - --disable-notifications

retry:
  enabled: true
  maxAttempts: 2

timeouts:
  explicit: 10          # seconds — used by WaitEngine
  pageLoad: 30          # seconds
```

That is the only configuration file needed. All fields have defaults — start with the minimum:

```yaml
execution:
  mode: local
  baseUrl: https://example.com

browser:
  name: chrome
```

---

### Step 3: Project Structure

```
your-project/
├── pom.xml
├── selenium-boot.yml
└── src/
    └── test/
        └── java/
            └── com/yourcompany/
                ├── conditions/
                │   └── AppConditions.java
                ├── pages/
                │   └── LoginPage.java
                └── tests/
                    └── LoginTest.java
```

---

### Step 4: Create a Page Object

Extend the framework's built-in `BasePage` — it provides wait-backed interaction helpers out of the box:

```java
package com.yourcompany.pages;

import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton   = By.id("login-btn");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String username, String password) {
        type(usernameField, username);
        type(passwordField, password);
        click(loginButton);
    }
}
```

`BasePage` provides: `click`, `type`, `getText`, `getAttribute`, `isDisplayed`, `withinFrame`, `withinFrameIndex`, `upload`. All backed by `WaitEngine` — no manual waits needed.

---

### Step 5: Write Your Tests

Extend `BaseTest` — that's all the setup needed:

```java
package com.yourcompany.tests;

import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.test.BaseTest;
import com.yourcompany.pages.LoginPage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class LoginTest extends BaseTest {

    @Test
    public void loginWithValidCredentials() {
        StepLogger.step("Open login page");
        open();

        StepLogger.step("Enter credentials and submit", true);
        new LoginPage(getDriver()).login("admin", "password123");

        assertTrue(getDriver().getCurrentUrl().contains("/dashboard"));
    }
}
```

**Rules:**
- Always extend `BaseTest`
- Never instantiate or quit `WebDriver` manually — the framework manages it
- Use `getDriver()` to access the current thread's driver instance
- Use `open()` to navigate to `baseUrl`, or `open("/path")` for a sub-path

---

### Step 6: Run Tests

```bash
mvn test
```

That's it. Selenium Boot handles driver creation, parallel execution, retries, screenshots, and report generation automatically.

---

### Step 7: View the Report

After execution, open the HTML report:

```
target/selenium-boot-report.html
```

The report includes:
- Pass rate gauge with colour coding
- Donut chart — pass/fail/skip distribution
- Per-test execution time and retry badges
- Step timeline per test
- Failure screenshots (base64 embedded, click to expand)
- Dark mode toggle

---

## @PreCondition — Session Caching

Eliminate repeated login boilerplate. Declare a condition once, cache the session, reuse it across tests:

```java
// 1. Define conditions
public class AppConditions extends BaseConditions {

    @ConditionProvider("loginAsAdmin")
    public void loginAsAdmin() {
        open("/");
        new LoginPage(getDriver()).login("admin", "secret");
    }
}
```

```
// 2. Register via SPI
src/test/resources/META-INF/services/com.seleniumboot.precondition.BaseConditions
→ com.yourcompany.conditions.AppConditions
```

```java
// 3. Use in tests
@Test
@PreCondition("loginAsAdmin")
public void viewDashboard() {
    open("/dashboard");  // session already established — no re-login
}

@Test
@PreCondition("loginAsAdmin")
public void editProfile() {
    open("/profile");    // session restored from cache
}
```

Cache is per-thread — safe for parallel execution. On retry, cache is invalidated and the condition re-runs fresh.

---

## WaitEngine Reference

`WaitEngine` uses the `timeouts.explicit` value from your config. Never use `Thread.sleep()`.

```java
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;

WebElement el  = WaitEngine.waitForVisible(By.id("submit-btn"));
WebElement btn = WaitEngine.waitForClickable(By.cssSelector(".next-btn"));
WaitEngine.waitForTitle("Dashboard");
WaitEngine.waitForUrlContains("/dashboard");
WaitEngine.waitForText(By.id("status"), "Complete");
WaitEngine.waitForPageLoad();
```

---

## Retry Reference

Add `@Retryable` to any `@Test` method to enable retry on failure. The number of retries is controlled by `retry.maxAttempts` in your config. Retries can be globally disabled with `retry.enabled: false`.

```java
@Retryable
@Test
public void flakyTest() {
    // retried up to maxAttempts times if it fails
}
```

---

## Remote Execution (Selenium Grid)

Update `selenium-boot.yml`:

```yaml
execution:
  mode: remote
  baseUrl: https://example.com
  gridUrl: http://localhost:4444/wd/hub
  parallel: methods
  threadCount: 4

browser:
  name: chrome
  headless: true
```

No code changes required — just config.

---

## Environment Profiles

Name your config files by environment and activate with a system property:

```
selenium-boot.yml          # default
selenium-boot-staging.yml  # staging profile
selenium-boot-prod.yml     # prod profile
```

```bash
mvn test -Denv=staging
```

---

## Extending the Framework

Selenium Boot exposes four extension points. All support both **Java SPI** (automatic discovery) and **programmatic registration**.

### Custom Driver Provider

```java
public class EdgeDriverProvider implements NamedDriverProvider {
    @Override public String browserName() { return "edge"; }
    @Override public WebDriver createDriver() { return new EdgeDriver(); }
}
```

Register via SPI (`META-INF/services/com.seleniumboot.driver.NamedDriverProvider`) or:
```java
DriverProviderRegistry.register(new EdgeDriverProvider());
```

### Custom Report Adapter

```java
public class SlackReportAdapter implements ReportAdapter {
    @Override public String getName() { return "slack"; }
    @Override public void generate(File metricsJson) { /* post to Slack */ }
}
```

Register via SPI (`META-INF/services/com.seleniumboot.reporting.ReportAdapter`) or:
```java
ReportAdapterRegistry.register(new SlackReportAdapter());
```

### Lifecycle Hooks

```java
public class TimingHook implements ExecutionHook {
    @Override
    public void onTestFailure(String testId, Throwable cause) {
        alerting.notify(testId, cause.getMessage());
    }
}
```

Available events: `onSuiteStart`, `onSuiteEnd`, `onTestStart`, `onTestEnd`, `onTestFailure`.

### Plugin System

Combine driver providers, report adapters, and hooks into a single deployable unit:

```java
public class MyPlugin implements SeleniumBootPlugin {
    @Override public String getName() { return "my-plugin"; }
    @Override public void onLoad(SeleniumBootConfig config) {
        ReportAdapterRegistry.register(new SlackReportAdapter());
    }
}
```

Declare minimum required framework version to prevent incompatibility:
```java
@Override public String minFrameworkVersion() { return "0.8.0"; }
```

---

## CI/CD Integration

Selenium Boot auto-detects CI environments and applies sensible defaults — no YAML changes required.

- `browser.headless` is forced to `true`
- `threadCount` is auto-derived from available CPU cores
- Docker/container flags (`--no-sandbox`, `--disable-dev-shm-usage`) are auto-applied to Chrome
- JUnit XML written to `target/surefire-reports/TEST-SeleniumBoot.xml` on every run

### Build Quality Gates

```yaml
ci:
  failOnPassRateBelow: 80   # fail build if pass rate drops below 80%
  maxFlakyTests: 3          # fail build if more than 3 tests were retried
```

---

## Project Status

### v0.9.1 — 2026-03-19

- **Alert wait fix** — `acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert` now use `WaitEngine.waitForAlert()` — no more `NoAlertPresentException` on slow pages
- **`WaitEngine.waitForAlert()`** — new explicit wait for browser alert presence
- **`BasePage.smartFind()`** — convenience wrapper; use inside page objects without passing the driver

---

### v0.9.0 — 2026-03-18

- **`BasePage` expanded** — dropdowns (`selectByText`, `selectByValue`, `selectByIndex`, `getSelectedOption`), alerts (`acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert`), mouse actions (`hover`, `doubleClick`, `rightClick`), scroll (`scrollTo`, `scrollToTop`, `scrollToBottom`), JS fallbacks (`jsClick`, `jsType`)

---

### v0.8.0 — 2026-03-17

- **`BasePage`** — page object base class: `click`, `type`, `getText`, `isDisplayed`, `withinFrame`, `withinFrameIndex`, `upload`
- **`SmartLocator`** — tries multiple `By` strategies in order, returns first visible element
- **`DownloadManager`** — `waitForFile`, `waitForAnyFile`, `clearDownloads` with partial-download detection
- **`ConsoleErrorCollector`** — JS console error capture (Chrome via WebDriver logs, Firefox via injected shim)
- **`@PreCondition`** — session-aware pre-conditions with cookie + localStorage caching per thread
- **`@ConditionProvider`** + `BaseConditions` — define named condition providers, registered via SPI
- **`@SeleniumBootApi`** — annotation marking stable public API with `since` version
- **`FrameworkVersion`** + `minFrameworkVersion()` — runtime version checks, plugin compatibility enforcement
- **Config additions** — `browser.downloadDir`, `browser.captureConsoleErrors`, `browser.failOnConsoleErrors`

---

### v0.7.0 — 2026-03-16

- **`StepLogger`** — named test steps with timestamps and optional per-step screenshots
- **Step timeline** — step-by-step execution timeline in the HTML report Failures tab
- **Tabbed HTML report** — Dashboard, Test Cases, and Failures tabs with collapsible rows
- **Browser lifecycle** — `browser.lifecycle: per-test | per-suite` configuration

---

### v0.6.0 — 2025-12-01

- **Advanced HTML report** — pass rate gauge, donut chart, slowest tests card, retry badges
- **Self-contained report** — screenshots base64-encoded, single file, no external dependencies

---

### v0.5.0 — 2025-10-15

- **Retry support** — `retry.enabled` + `retry.maxAttempts` in `selenium-boot.yml`
- **`@Retryable`** — per-method retry override
- **Retry metrics** — retry counts tracked in `ExecutionMetrics` and exported to JSON

---

### v0.4.0 — 2025-08-20

- **CI auto-detection** — GitHub Actions, Jenkins, CircleCI, GitLab CI; forces headless, tunes thread count
- **Container detection** — Docker and Kubernetes; auto-applies Chrome container flags
- **JUnit XML reporter** — `target/surefire-reports/TEST-SeleniumBoot.xml`
- **Build quality gates** — `BuildThresholdEnforcer` enforces pass-rate and flaky-test thresholds
- **CI templates** — GitHub Actions workflow and Jenkinsfile included

---

### v0.3.0 — 2025-06-10

- **Plugin system** — `SeleniumBootPlugin` + `PluginRegistry` with SPI discovery
- **Custom driver providers** — `NamedDriverProvider` + `DriverProviderRegistry`
- **Custom report adapters** — `ReportAdapter` + `ReportAdapterRegistry`
- **Lifecycle hooks** — `ExecutionHook` + `HookRegistry`
- **`SeleniumBootDefaults`** — programmatic config defaults for shared test-base JARs

---

### v0.2.0 — 2025-04-05

- **`WaitEngine`** — fluent explicit wait API
- **Thread-safe config** — `SeleniumBootContext` with `AtomicReference`
- **Session semaphore** — `maxActiveSessions` cap with fair wait
- **Global retry** — `retry.enabled: true` retries all tests without `@Retryable`

---

### v0.1.0 — 2025-02-01

- Initial release — Chrome/Firefox, TestNG integration, `selenium-boot.yml`, basic HTML report, screenshot on failure

---

## Sample Project

A working demo project covering all framework features is available at:
**[github.com/seleniumboot/selenium-boot-test](https://github.com/seleniumboot/selenium-boot-test)**

---

## Documentation

Full documentation at **[seleniumboot.github.io/selenium-boot](https://seleniumboot.github.io/selenium-boot)**

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Disclaimer

Selenium Boot is an independent open-source project and is not affiliated with Selenium or the Spring Framework.
