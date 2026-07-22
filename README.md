# Selenium Boot

**The Spring Boot of Selenium — Playwright-inspired APIs, zero setup, and enterprise features, without hiding Selenium**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.seleniumboot/selenium-boot)](https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![PRs welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Good first issues](https://img.shields.io/github/issues/seleniumboot/selenium-boot/good%20first%20issue?label=good%20first%20issues&color=7057ff)](https://github.com/seleniumboot/selenium-boot/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)

**[Documentation](https://docs.seleniumboot.com) · [Sample Project](https://github.com/seleniumboot/selenium-boot-test) · [Changelog](#project-status)**

---

## Quickstart — a green test in 60 seconds

Three files. Copy them as-is and `mvn test` goes green against a real Chrome.

**Prerequisites:** Java 17+, Maven 3.8+, Chrome installed. No WebDriver binaries — Selenium Manager fetches them.

**1. `pom.xml`**

```xml
<properties>
    <maven.compiler.release>17</maven.compiler.release>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.seleniumboot</groupId>
        <artifactId>selenium-boot</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

**2. `selenium-boot.yml`** — project root, next to `pom.xml`. This is the complete minimum; every key below is required.

```yaml
execution:
  mode: local
  baseUrl: https://example.com

browser:
  name: chrome

timeouts:
  explicit: 10
  pageLoad: 30
```

**3. `src/test/java/SmokeTest.java`**

```java
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class SmokeTest extends BaseTest {

    @Test
    public void opensThePage() {
        open();
        assertTrue(getDriver().getTitle().contains("Example Domain"));
    }
}
```

Then:

```bash
mvn test
```

No driver setup, no teardown, no waits, no `WebDriver` to manage — `BaseTest` owns the lifecycle. The HTML report lands at `target/selenium-boot-report.html`.

Next: [the full Getting Started walkthrough](#getting-started) adds page objects, parallel execution, and reporting.

---

> **AI-powered test authoring for Selenium Boot users**
> Use **seleniumboot-mcp** to let Claude / GitHub Copilot control a real browser, record your session, and generate ready-to-run Selenium Boot test code — TestNG, JUnit 5, Page Object, Gherkin, or C# NUnit.
> ```
> pip install seleniumboot-mcp
> ```
> [PyPI](https://pypi.org/project/seleniumboot-mcp/) · [GitHub](https://github.com/seleniumboot/selenium-mcp) · 84 tools · self-healing locators · codegen for Java / Python / C# / Playwright

---

## Overview

Selenium Boot is a zero-boilerplate, production-ready automation framework for Java Selenium, inspired by the philosophy of Spring Boot.

It eliminates repetitive boilerplate by providing sensible defaults, a standardized project structure, and a convention-over-configuration approach — while keeping Selenium fully visible and accessible.

### Design Philosophy

Selenium Boot is **the Spring Boot of Java test automation** — and that positioning is deliberately layered:

1. **Opinionated core (primary).** Convention over configuration, zero boilerplate by default. Add one dependency, extend `BaseTest` / `BasePage`, and the framework has already made the sensible decisions — driver lifecycle, waits, retries, reporting, CI wiring. `selenium-boot.yml` stays short — a handful of required keys, and `SeleniumBootDefaults` covers the rest.
2. **Never hides Selenium (the constraint).** Unlike heavier abstractions, Selenium Boot never takes the raw `WebDriver` away from you. When the conventions don't fit, drop straight down to `WebDriver` / `By` / `WebElement`. Opinionated without being a cage.
3. **Extensible toolkit (the escape hatch).** An SPI/registry plugin system (`DriverProviderRegistry`, `PluginRegistry`, `ReportAdapterRegistry`) makes it modular for the power users who need it — serving the opinionated core, not replacing it. Most users never touch it.

**Already invested in Selenium?** Selenium Boot gives you the productivity features people love in Playwright — **accessibility-first locators** (`getByRole` / `getByLabel` / `getByText`), **auto-waiting** so `Thread.sleep()` disappears, and **web-first assertions** — while keeping your existing Selenium / Java / TestNG stack, team skills, and Selenium Grid. You get the modern ergonomics without leaving the ecosystem you've already built on, and without ever hiding raw Selenium.

**Why not just build your own framework?** Most teams already have a home-grown `BaseTest` + `DriverFactory` + wait-utils they've rewritten a dozen times. Selenium Boot *is* that framework — maintained, tested, parallel-safe, and documented — so the driver lifecycle, retries, reporting, and CI wiring stop being unpaid infrastructure you own forever. You keep your test code; you delete the plumbing.

> Selenium Boot is the Spring Boot of Selenium — zero setup, smarter defaults, Playwright-inspired APIs, and enterprise features, without hiding Selenium.

---

## What You Get Out of the Box

Outcomes first — the API that delivers each one is named so you can find it in the docs.

- **Never write driver setup or teardown again** — automatic WebDriver lifecycle, thread-safe per test
- **Never write `Thread.sleep()` again** — auto-waiting `WaitEngine` with 10+ built-in conditions
- **Tests survive CSS and DOM refactors** — accessibility-first locators (`getByRole`, `getByText`, `getByLabel`, `getByPlaceholder`, `getByTestId`, `getByAltText`, `getByTitle`) plus a `SmartLocator` fallback that tries multiple strategies
- **Flaky tests stop failing your build** — automatic retry via `@Retryable`
- **Run your whole suite in parallel, safely** — thread-isolated drivers, `parallel` in one YAML line
- **Switch environments without touching code** — YAML config with environment profile switching
- **See exactly why a test failed** — screenshot auto-captured on failure and embedded in the report
- **Hand stakeholders a report they'll actually read** — HTML dashboard with pass-rate gauge, donut chart, slowest tests, step timeline, dark mode
- **Write pages, not plumbing** — `BasePage` with wait-backed `click`, `type`, `getText`, `isDisplayed`, iFrame helpers, file upload
- **Log in once, reuse the session** — `@PreCondition` with automatic cookie + localStorage caching
- **Catch JavaScript errors your users would hit** — `ConsoleErrorCollector` (Chrome via logs, Firefox via shim)
- **File download testing that just works** — `DownloadManager` polls the download dir and handles partial files
- **Read the test like a spec** — `StepLogger` named steps with timestamps and per-step screenshots
- **Test UI and API in the same suite** — `BaseApiTest` + fluent `ApiClient` with auth, schema validation, JSONPath; hybrid UI + API tests
- **Accessibility testing in one line** — `accessibility().withTags("wcag2a","wcag21aa").run()`; axe-core bundled in the JAR, no extra dependency
- **Extend it without forking it** — Java SPI plugins for custom browser providers, report adapters, lifecycle hooks
- **CI that configures itself** — auto-detects GitHub Actions, Jenkins, CircleCI; forces headless, emits JUnit XML

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
    <version>3.2.0</version>
</dependency>
```

Also pin the compiler plugin and add Surefire so `mvn test` discovers TestNG tests:

```xml
<properties>
    <maven.compiler.release>17</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

> Pinning `maven-compiler-plugin` matters: on Maven 3.8.x and earlier the default compiler level falls back to source/target 5, and the build fails with *"Source option 5 is no longer supported"* on any modern JDK.

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

That is the only configuration file needed, and it is **required** — Selenium Boot fails fast at startup if it is missing. Most fields are optional, but `execution.mode`, `browser.name` (or `browser.matrix`), and both `timeouts` values are validated and must be present:

```yaml
execution:
  mode: local
  baseUrl: https://example.com

browser:
  name: chrome

timeouts:
  explicit: 10
  pageLoad: 30
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

## API Testing

Selenium Boot supports **pure API tests** and **hybrid UI + API tests** — same framework, same config, same HTML report.

### Pure API Tests

Extend `BaseApiTest` instead of `BaseTest`. No browser is launched.

```java
public class UserApiTest extends BaseApiTest {

    @Test
    public void getUserById() {
        ApiClient.get("https://api.example.com/users/1")
                .send()
                .assertStatus(200)
                .assertJson("$.name", "John Doe");
    }
}
```

### `ApiClient` — Fluent HTTP Client

```java
// GET
ApiClient.get("/api/users").send();

// POST with body
ApiClient.post("/api/users")
        .body(Map.of("name", "Alice", "email", "alice@example.com"))
        .send()
        .assertStatus(201);

// Custom header
ApiClient.get("/api/orders")
        .header("X-Request-ID", "abc123")
        .send();

// Different base URL for one request
ApiClient.to("https://other-service.com").get("/health").send();
```

Configure the default base URL in `selenium-boot.yml`:

```yaml
api:
  baseUrl: https://api.example.com
  timeoutSeconds: 30
  logBody: false   # set true to include body in step timeline
```

### `ApiResponse` — Assertions and Extraction

```java
ApiResponse res = ApiClient.get("/api/users/1").send();

res.assertStatus(200);
res.assertBodyContains("Alice");
res.assertJson("$.name", "Alice");

// Extract values
String name  = res.json("$.name");
int    id    = res.json("$.id", Integer.class);
User   user  = res.asObject(User.class);

// Fluent chaining
res.assertStatus(200)
   .assertJson("$.name", "Alice")
   .assertSchema("schemas/user.json");
```

### Authentication

**Bearer token:**
```java
ApiClient.get("/api/me")
        .auth(ApiAuth.bearerToken("my-token"))
        .send();
```

**Basic auth:**
```java
ApiClient.get("/api/admin")
        .auth(ApiAuth.basicAuth("user", "pass"))
        .send();
```

**Set auth once for the entire suite** — all requests use it automatically:
```java
@BeforeSuite
public void authenticate() {
    ApiResponse login = ApiClient.post("/api/auth/login")
            .body(Map.of("username", "admin", "password", "pass"))
            .send();
    ApiClient.setGlobalAuth(ApiAuth.bearerToken(login.json("$.token")));
}
```

**OAuth2 client credentials** — token fetched and cached automatically:
```java
ApiClient.setGlobalAuth(ApiAuth.oauth2(
    "https://auth.example.com/token",
    System.getenv("CLIENT_ID"),
    System.getenv("CLIENT_SECRET")
));
```

**Config-based auth with `@UseAuth`** — define strategies in YAML, apply per test:
```yaml
api:
  auth:
    adminToken:
      type: bearer
      token: ${ADMIN_TOKEN}       # resolved from env var
    serviceAccount:
      type: oauth2
      tokenUrl: https://auth.example.com/token
      clientId: ${CLIENT_ID}
      clientSecret: ${CLIENT_SECRET}
```

```java
@Test
@UseAuth("adminToken")
public void createUser() {
    ApiClient.post("/api/users").body(...).send().assertStatus(201);
}
```

### Schema Validation

Validate response structure against a JSON Schema file:

```java
ApiClient.get("/api/users/1")
        .send()
        .assertStatus(200)
        .assertSchema("schemas/user.json");
```

Place schema files under `src/test/resources/schemas/`. Requires one additional dependency:

```xml
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.4.3</version>
</dependency>
```

### Hybrid UI + API Tests

Mix API calls and browser interactions in the same test via `apiClient()` in `BaseTest`:

```java
public class CheckoutTest extends BaseTest {

    @Test
    public void placeOrder() {
        // Set up via API (fast, no UI navigation)
        String orderId = apiClient().post("/api/orders")
                .body(Map.of("productId", 42, "qty", 1))
                .send()
                .assertStatus(201)
                .json("$.orderId");

        // Verify in the UI
        open("/orders/" + orderId);
        Assert.assertEquals(getText(By.id("status")), "Pending");
    }
}
```

### Scenario & Suite Context

Share state within a test or across tests without static fields:

```java
// ScenarioContext — lives for one test, auto-cleared after
ctx().set("token", loginRes.json("$.token"));
String token = ctx().get("token");

// SuiteContext — survives between tests, thread-safe
suiteCtx().set("createdUserId", res.json("$.id"));   // in test 1
String userId = suiteCtx().get("createdUserId");      // in test 2
```

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

**Current release: v3.2.0** — three new `WaitEngine` conditions: `waitForAttribute` (exact-match attribute), `waitForUrlMatches`, and `waitForTextMatches` (regex).

See the full version history in **[CHANGELOG.md](CHANGELOG.md)**.

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

Contributions are warmly welcome — Selenium Boot is opinionated, and contributions that align with its philosophy (zero boilerplate, convention over configuration, never hide Selenium) help the whole community.

**New here?** The best place to start:

- 🙌 [**Good first issues**](https://github.com/seleniumboot/selenium-boot/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) — scoped, self-contained tasks
- 🤝 [**Help wanted**](https://github.com/seleniumboot/selenium-boot/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22) — larger pieces we'd love a hand with
- 🗺️ [**Roadmap**](ROADMAP.md) — where the project is heading and where help fits
- 💬 [**Discussions**](https://github.com/seleniumboot/selenium-boot/discussions) — questions and feature ideas

Then read [CONTRIBUTING.md](CONTRIBUTING.md) for dev setup, the PR checklist, and the backward-compatibility policy. Bug reports and feature requests both have [issue templates](https://github.com/seleniumboot/selenium-boot/issues/new/choose) to guide you.

---

## Disclaimer

Selenium Boot is an independent open-source project and is not affiliated with Selenium or the Spring Framework.
