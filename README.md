# Selenium Boot

**An Opinionated, Spring Boot–Inspired Framework for Java QA Automation**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.seleniumboot/selenium-boot)](https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

## Overview

Selenium Boot is an opinionated, production-ready automation framework for Java Selenium, inspired by the philosophy of Spring Boot.

It eliminates repetitive boilerplate by providing sensible defaults, a standardized project structure, and a convention-over-configuration approach — while keeping Selenium fully visible and accessible.

---

## What You Get Out of the Box

- Automatic WebDriver lifecycle management (no setup/teardown boilerplate)
- YAML-based configuration with environment profile switching
- Parallel execution enabled by default
- Smart explicit waits via `WaitEngine`
- Automatic retry for flaky tests via `@Retryable`
- Screenshot capture on failure
- HTML execution report with pass/fail/skip breakdown
- One-command execution via Maven

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
    <version>0.4.0</version>
</dependency>

<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.9.0</version>
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
                ├── pages/
                │   ├── BasePage.java
                │   └── LoginPage.java
                └── tests/
                    └── LoginTest.java
```

---

### Step 4: Create a Base Page (Optional but Recommended)

```java
package com.yourcompany.pages;

import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class BasePage {

    protected final WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
    }

    protected WebElement waitForVisible(By locator) {
        return WaitEngine.waitForVisible(locator);
    }

    protected WebElement waitForClickable(By locator) {
        return WaitEngine.waitForClickable(locator);
    }
}
```

---

### Step 5: Create a Page Object

```java
package com.yourcompany.pages;

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
        waitForVisible(usernameField).sendKeys(username);
        driver.findElement(passwordField).sendKeys(password);
        waitForClickable(loginButton).click();
    }
}
```

---

### Step 6: Write Your Tests

Extend `BaseTest` — that's all the setup needed:

```java
package com.yourcompany.tests;

import com.seleniumboot.listeners.Retryable;
import com.seleniumboot.test.BaseTest;
import com.yourcompany.pages.LoginPage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class LoginTest extends BaseTest {

    @Test
    public void loginWithValidCredentials() {
        open();                                          // navigates to baseUrl
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("admin", "password123");
        assertTrue(getDriver().getCurrentUrl().contains("/dashboard"));
    }

    @Test
    public void loginToSpecificPath() {
        open("/login");                                  // navigates to baseUrl + /login
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("admin", "password123");
        assertTrue(getDriver().getTitle().contains("Dashboard"));
    }

    @Retryable
    @Test
    public void flakyLoginTest() {
        open("/login");
        // This test will retry up to maxAttempts times on failure
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login("admin", "password123");
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

### Step 7: Run Tests

```bash
mvn test
```

That's it. Selenium Boot handles driver creation, parallel execution, retries, screenshots, and report generation automatically.

---

### Step 8: View the Report

After execution, open the HTML report:

```
target/selenium-boot-report.html
```

The report includes:
- Suite-level summary (total / passed / failed / skipped)
- Per-test execution time
- Pass/fail/skip status with color coding
- Slowest test detection
- Failure screenshots (linked inline)

---

## WaitEngine Reference

`WaitEngine` uses the `timeouts.explicit` value from your config. Never use `Thread.sleep()`.

```java
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;

// Wait for an element to be visible, then return it
WebElement el = WaitEngine.waitForVisible(By.id("submit-btn"));

// Wait for an element to be clickable, then return it
WebElement btn = WaitEngine.waitForClickable(By.cssSelector(".next-btn"));

// Wait for page title to match exactly
WaitEngine.waitForTitle("Dashboard");

// Wait for URL to contain a string
WaitEngine.waitForUrlContains("/dashboard");
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

Selenium Boot 0.3.0 exposes four extension points. All support both **Java SPI** (automatic discovery) and **programmatic registration**.

---

### Custom Driver Provider

Support browsers beyond Chrome and Firefox (Edge, Safari, Opera, etc.) without forking the framework.

**1. Implement `NamedDriverProvider`:**

```java
public class EdgeDriverProvider implements NamedDriverProvider {

    @Override
    public String browserName() { return "edge"; }

    @Override
    public WebDriver createDriver() {
        EdgeOptions options = new EdgeOptions();
        return new EdgeDriver(options);
    }
}
```

**2a. Register via SPI** — create `src/main/resources/META-INF/services/com.seleniumboot.driver.NamedDriverProvider` containing:
```
com.yourcompany.driver.EdgeDriverProvider
```

**2b. Or register programmatically** (before framework boot):
```java
DriverProviderRegistry.register(new EdgeDriverProvider());
```

**3. Set in config:**
```yaml
browser:
  name: edge
```

---

### Custom Report Adapter

Deliver test results to any destination — Slack, email, Allure, etc.

```java
public class SlackReportAdapter implements ReportAdapter {

    @Override
    public String getName() { return "slack"; }

    @Override
    public void generate(File metricsJson) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(metricsJson);
        int failed = root.get("failedTests").asInt();
        if (failed > 0) slackClient.post("❌ " + failed + " test(s) failed");
    }
}
```

Register via SPI (`com.seleniumboot.reporting.ReportAdapter`) or:
```java
ReportAdapterRegistry.register(new SlackReportAdapter());
```

The built-in HTML report is always generated; custom adapters run after it.

---

### Lifecycle Hooks

React to suite and test events without subclassing any framework class.

```java
public class TimingHook implements ExecutionHook {

    @Override
    public void onTestEnd(String testId, String status) {
        observability.record(testId, status);
    }

    @Override
    public void onTestFailure(String testId, Throwable cause) {
        alerting.notify(testId, cause.getMessage());
    }
}
```

Available events: `onSuiteStart`, `onSuiteEnd`, `onTestStart(testId)`, `onTestEnd(testId, status)`, `onTestFailure(testId, cause)`.

Register via SPI (`com.seleniumboot.hooks.ExecutionHook`) or:
```java
HookRegistry.register(new TimingHook());
```

---

### Plugin System

Plugins combine driver providers, report adapters, and hooks into a single deployable unit, and gain access to the full framework config at load time.

```java
public class MyPlugin implements SeleniumBootPlugin {

    @Override
    public String getName() { return "my-plugin"; }

    @Override
    public void onLoad(SeleniumBootConfig config) {
        // read config, register adapters/hooks/providers
        ReportAdapterRegistry.register(new SlackReportAdapter());
        HookRegistry.register(new TimingHook());
    }

    @Override
    public void onUnload() {
        // flush resources, close connections
    }
}
```

Register via SPI (`com.seleniumboot.extension.SeleniumBootPlugin`) or:
```java
PluginRegistry.register(new MyPlugin(), config);
```

---

### Framework-safe Config Defaults

Override framework defaults programmatically — ideal for shared test-base JARs that establish org-wide baselines, which individual projects override via their own `selenium-boot.yml`.

```java
// Call this before FrameworkBootstrap runs (e.g., in a static initializer)
SeleniumBootDefaults.set("browser.name", "edge");
SeleniumBootDefaults.set("timeouts.explicit", 15);
SeleniumBootDefaults.set("execution.maxActiveSessions", 10);
```

YAML values always win over defaults set here.

**Supported keys:** `browser.name`, `timeouts.explicit`, `timeouts.pageLoad`, `execution.maxActiveSessions`, `execution.threadCount`, `retry.maxAttempts`.

---

## CI/CD Integration

Selenium Boot auto-detects CI environments and applies sensible defaults — no YAML changes required.

### What Happens Automatically in CI

When `CI`, `GITHUB_ACTIONS`, `JENKINS_URL`, or any other well-known CI env var is set:

- `browser.headless` is forced to `true` (CI agents have no display)
- `threadCount` is auto-derived from available CPU cores (when left at default `1` with parallel mode enabled)
- Docker/container flags (`--no-sandbox`, `--disable-dev-shm-usage`) are auto-applied to Chrome when running inside a container

### JUnit XML Output

Every run produces `target/surefire-reports/TEST-SeleniumBoot.xml` — parsed natively by Jenkins, GitHub Actions, GitLab CI, and most CI systems without plugins.

### Build Quality Gates

Optionally fail the build if test quality drops below a threshold. Add to `selenium-boot.yml`:

```yaml
ci:
  failOnPassRateBelow: 80   # fail build if pass rate drops below 80%
  maxFlakyTests: 3          # fail build if more than 3 tests were retried before passing
```

Both thresholds are disabled by default (`failOnPassRateBelow: 0`, `maxFlakyTests: -1`).

### CI Templates

Ready-to-use templates are included:

| Template | Location |
|---|---|
| GitHub Actions | `.github/workflows/selenium-boot.yml` |
| Jenkins | `ci/Jenkinsfile` |

Both templates handle Chrome setup, test execution, artifact upload (HTML report, JUnit XML, metrics JSON), and test result publishing automatically.

---

## Project Status

**v0.4.0 – CI/CD & Enterprise Readiness**

Seamless integration into enterprise pipelines — zero config changes needed in most CI environments:

- **CI auto-detection** — `CiEnvironmentDetector` identifies GitHub Actions, Jenkins, CircleCI, GitLab CI, Travis, TeamCity, and Bitbucket Pipelines; forces headless and tunes thread count automatically
- **Container detection** — detects Docker (via `/.dockerenv`) and Kubernetes (via `KUBERNETES_SERVICE_HOST`); auto-applies `--no-sandbox`, `--disable-dev-shm-usage` to Chrome
- **JUnit XML report** — `JUnitXmlReporter` writes `target/surefire-reports/TEST-SeleniumBoot.xml` on every run; parsed natively by all major CI platforms
- **Build quality gates** — `BuildThresholdEnforcer` enforces pass-rate and flaky-test thresholds; breaching a gate throws `BuildQualityGateException` which fails the Maven build
- **CI templates** — `.github/workflows/selenium-boot.yml` (GitHub Actions) and `ci/Jenkinsfile` (Jenkins) included out of the box with report upload and test result publishing
- **Unit tests** — all Phase 4 classes covered: `CiEnvironmentDetectorTest`, `BuildThresholdEnforcerTest`, `JUnitXmlReporterTest`

---

**v0.3.0 – Extensibility Release**

Full extensibility layer — extend any part of the framework without forking it:

- **Plugin system** — `SeleniumBootPlugin` + `PluginRegistry`; plugins discovered via Java SPI, activated at framework boot, unloaded after suite finish
- **Custom driver providers** — `NamedDriverProvider` + `DriverProviderRegistry`; register Edge, Safari, or any browser provider via SPI or programmatically; takes precedence over built-in Chrome/Firefox providers
- **Custom report adapters** — `ReportAdapter` + `ReportAdapterRegistry`; deliver results to Slack, email, Allure, or any destination; built-in HTML adapter always runs first
- **Lifecycle hooks** — `ExecutionHook` + `HookRegistry`; react to `onSuiteStart`, `onSuiteEnd`, `onTestStart`, `onTestEnd`, `onTestFailure` without subclassing anything
- **Framework-safe defaults** — `SeleniumBootDefaults`; programmatically set config defaults before YAML is applied; ideal for shared test-base JARs
- **SPI descriptors** — four `META-INF/services/` files ready to populate for zero-config extension discovery
- **Unit tests** — all five extension points covered: `PluginRegistryTest`, `HookRegistryTest`, `ReportAdapterRegistryTest`, `DriverProviderRegistryTest`, `SeleniumBootDefaultsTest`

---

**v0.2.0 – Enhancements Release**

Stability and usability improvements across the core framework:

- **Thread-safety** — `SeleniumBootContext` now uses `AtomicReference` for lock-free, race-free config publishing
- **Session management** — `DriverManager` session limit uses a fair `Semaphore` (30s timeout) instead of fail-fast; parallel tests wait for a slot rather than erroring
- **Global retry** — `retry.enabled=true` in YAML now retries all tests without requiring `@Retryable` per method
- **WaitEngine** — expanded with `waitForInvisible`, `waitForStaleness`, `waitForText`, `waitForAttributeContains`, `waitForPageLoad`, and a custom `wait(ExpectedCondition)` escape hatch
- **BasePage** — pre-built interaction helpers (`click`, `type`, `getText`, `getAttribute`, `isDisplayed`) eliminate boilerplate in page objects
- **Screenshots embedded** — failure screenshots are now base64-encoded into the HTML report (click to expand); no broken links when sharing reports
- **Metrics history** — timestamped JSON copies written to `target/metrics-history/` for CI archiving; primary `selenium-boot-metrics.json` unchanged
- **Config fallback** — `ConfigurationLoader` supports `-Dselenium.boot.config=`, working-directory file, then classpath (priority order)
- **JUnit 5 support** — `SeleniumBootExtension`, `@EnableSeleniumBoot`, and `BaseJUnit5Test` allow JUnit 5 projects to use the framework alongside TestNG users
- **HTML report template** — extracted to `src/main/resources/report-template.html`; editable without recompiling
- **Unit tests** — framework internals covered by `ExecutionMetricsTest`, `SeleniumBootContextTest`, `ConfigurationLoaderTest`, `RetryListenerTest`

---

**v0.1.0 – Initial Release**

Core framework is implemented and functional. Includes WebDriver lifecycle management, YAML-based configuration, parallel execution, automatic retry via `@Retryable`, explicit waits, screenshot capture on failure, and HTML execution reports with pass/fail/skip tracking.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).

---

## Contributing

See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md).

---

## Disclaimer

Selenium Boot is an independent open-source project and is not affiliated with Selenium or the Spring Framework.
