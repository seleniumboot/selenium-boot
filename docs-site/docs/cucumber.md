---
id: cucumber
title: BDD / Cucumber
sidebar_position: 11
---

# BDD / Cucumber Integration

Selenium Boot integrates with Cucumber 7 out of the box. Driver lifecycle, the HTML report step timeline, screenshots on failure, metrics, and all framework features work automatically — no boilerplate in your step definitions.

---

## Setup

Add Cucumber to your project alongside Selenium Boot:

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>1.11.0</version>
</dependency>

<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.20.1</version>
</dependency>

<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-testng</artifactId>
    <version>7.20.1</version>
</dependency>
```

---

## Project structure

```
src/
└── test/
    ├── java/
    │   └── com/yourcompany/
    │       ├── bdd/
    │       │   ├── CucumberRunner.java       ← runner class
    │       │   └── steps/
    │       │       ├── LoginSteps.java        ← extend BaseCucumberSteps
    │       │       └── NavigationSteps.java
    └── resources/
        ├── features/
        │   └── login.feature
        └── cucumber.properties               ← for IDE single-scenario runs
```

---

## Runner class

Annotate with `@CucumberOptions` and extend `BaseCucumberTest`. No other code needed:

```java
@CucumberOptions(
    features = "src/test/resources/features",
    glue     = {"com.yourcompany.bdd.steps", "com.seleniumboot.cucumber"},
    plugin   = {"pretty", "com.seleniumboot.cucumber.CucumberStepLogger"}
)
public class CucumberRunner extends BaseCucumberTest {}
```

`"com.seleniumboot.cucumber"` in `glue` is required — it tells Cucumber where to find `CucumberHooks`, which manages the driver lifecycle.

`CucumberStepLogger` in `plugin` streams Gherkin step names into the Selenium Boot HTML report step timeline.

---

## Step definitions

Extend `BaseCucumberSteps` to get `getDriver()`, `open()`, `$()`, `assertThat()`:

```java
public class LoginSteps extends BaseCucumberSteps {

    private LoginPage loginPage;

    @Given("the user is on the login page")
    public void onLoginPage() {
        open();                                 // navigates to execution.baseUrl
        loginPage = new LoginPage(getDriver());
    }

    @When("they login as {string} with password {string}")
    public void login(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("the dashboard is visible")
    public void dashboardVisible() {
        assertThat(By.id("dashboard")).isVisible();   // auto-retrying assertion
    }
}
```

`BaseCucumberSteps` provides:

| Method | Description |
|---|---|
| `getDriver()` | Current thread's `WebDriver` |
| `getWait()` | `WebDriverWait` using `timeouts.explicit` from `selenium-boot.yml` |
| `open()` | Navigate to `execution.baseUrl` |
| `open(path)` | Navigate to `baseUrl + path` |
| `$(css)` | Chainable fluent locator |
| `$(By)` | Chainable fluent locator |
| `assertThat(By)` | Auto-retrying assertion |
| `assertThat(Locator)` | Auto-retrying assertion on a locator chain |
| `getScenario()` | The current Cucumber `Scenario` object |

---

## Feature files

Standard Gherkin — no framework-specific syntax:

```gherkin title="src/test/resources/features/login.feature"
Feature: User Login

  Scenario: Valid credentials grant access
    Given the user is on the login page
    When they login as "admin" with password "secret"
    Then the dashboard is visible

  Scenario Outline: Multiple accounts can log in
    Given the user is on the login page
    When they login as "<username>" with password "<password>"
    Then the dashboard is visible

    Examples:
      | username | password |
      | admin    | secret   |
      | editor   | pass123  |
```

Each Scenario Outline example row produces a separate entry in the HTML report with its own step timeline and screenshot.

---

## IDE single-scenario execution

When running a single scenario from the IDE (right-click → Run), the IDE uses its own runner and doesn't read `@CucumberOptions`. Add a `cucumber.properties` file so `CucumberHooks` is always discovered:

```properties title="src/test/resources/cucumber.properties"
cucumber.glue=com.yourcompany.bdd.steps,com.seleniumboot.cucumber
cucumber.plugin=pretty,com.seleniumboot.cucumber.CucumberStepLogger
cucumber.monochrome=true
```

---

## Retry

### Global retry

Enable retry in `selenium-boot.yml` — all scenarios that fail will be retried automatically:

```yaml title="selenium-boot.yml"
retry:
  enabled: true
  maxAttempts: 1   # 1 retry = 2 total attempts per scenario
```

### Per-scenario retry tag

Override the global config for individual scenarios using the `@retryable` or `@retryable=N` tag:

```gherkin
# Use global retry.maxAttempts from selenium-boot.yml
@retryable
Scenario: Login sometimes flakes on slow CI
  Given the user is on the login page
  When they submit valid credentials
  Then the dashboard is visible

# Exactly 2 retries regardless of global config
@retryable=2
Scenario: Very flaky third-party widget
  Given the widget is loaded
  Then it should display the correct value
```

Tag formats:

| Tag | Behaviour |
|---|---|
| `@retryable` | Retry using `retry.maxAttempts` from config |
| `@retryable=N` | Retry exactly N times (overrides config) |

### How it works

When a scenario fails, the **entire scenario reruns from step 1** with a fresh driver. The app is in a clean state for every retry attempt.

Retried scenarios show a **↻ 1x** badge in the HTML report. The final status (PASSED or FAILED after all attempts) is what appears in the report.

---

## Run via Maven

```bash
# Run all Cucumber scenarios
mvn test -Dtest=CucumberRunner

# Run a specific feature file
mvn test -Dtest=CucumberRunner -Dcucumber.features=src/test/resources/features/login.feature

# Run scenarios tagged @smoke
mvn test -Dtest=CucumberRunner -Dcucumber.filter.tags="@smoke"
```

---

## What's automatic

`CucumberHooks` (in the `com.seleniumboot.cucumber` glue package) handles everything per scenario:

| Event | What happens |
|---|---|
| Scenario start | Driver created, metrics timer started, test ID registered |
| Step execution | `CucumberStepLogger` logs each step name + pass/fail status into the HTML report timeline |
| Scenario failure | Screenshot captured and embedded in both the Selenium Boot report and Cucumber's own HTML report |
| Scenario end | Driver quit, metrics recorded, status (PASSED / FAILED / SKIPPED) written |
| Suite end | `SuiteExecutionListener.onFinish()` generates the full HTML report, flakiness radar, and JSON export |

---

## Parallel execution

Set `parallel` and `threadCount` in `selenium-boot.yml` — the framework's ThreadLocal driver isolation makes Cucumber scenarios thread-safe:

```yaml title="selenium-boot.yml"
execution:
  parallel: methods
  threadCount: 4
  maxActiveSessions: 4
```

Each scenario runs on its own thread with its own driver instance.

---

## Mixing Cucumber and TestNG in one suite

Selenium Boot supports both in the same Maven invocation. The HTML report combines TestNG test results and Cucumber scenario results into a single dashboard. `TestExecutionListener` automatically skips recording for Cucumber runner tests to avoid duplicate entries.

---

## Attaching data to the Cucumber report

Access the current `Scenario` object from any step definition to attach screenshots, text, or JSON to Cucumber's own HTML report:

```java
public class MySteps extends BaseCucumberSteps {

    @Then("attach current screenshot")
    public void attachScreenshot() {
        String base64 = ScreenshotManager.captureAsBase64();
        if (base64 != null) {
            getScenario().attach(
                Base64.getDecoder().decode(base64),
                "image/png",
                "Current state"
            );
        }
    }
}
```
