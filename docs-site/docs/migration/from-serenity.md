---
description: "Coming from Serenity BDD to Selenium Boot: Screenplay and @Steps versus page objects and BaseTest, reporting differences, and what carries over (Cucumber features)."
id: from-serenity
title: Coming from Serenity BDD
sidebar_label: Coming from Serenity BDD
sidebar_position: 5
---

# Coming from Serenity BDD

Serenity BDD is a mature framework with a distinctive model: Screenplay or `@Steps` libraries for structure, and "living documentation" reports built from them. If that model is working for your team, keep it.

This page is for the team that finds the layering heavier than it needs to be, or that wants a plainer page-object style with Cucumber on top. It maps what you write today to what you would write in Selenium Boot, and says plainly what does not carry over.

:::info The good news
**Your Gherkin feature files carry over unchanged.** Selenium Boot runs standard Cucumber, so `.feature` files need no edits. What you rewrite is the Java behind the steps.
:::

---

## The structural difference

| | Serenity BDD | Selenium Boot |
|---|---|---|
| Step definitions | Delegate to `@Steps` classes or Screenplay `Actor`s | Plain Cucumber step classes extending `BaseCucumberSteps` |
| Where the logic lives | `Task`, `Question`, `@Steps` libraries | Page objects extending `BasePage` |
| Driver | `@Managed` / `WebDriver` supplied by Serenity | Created and quit per scenario by the framework |
| Configuration | `serenity.conf` / `serenity.properties` | `selenium-boot.yml` (optional) |
| Report | Serenity living documentation | Selenium Boot HTML report |

Selenium Boot does not have an Actor/Task/Question layer. If your team likes that separation, keep it in your own code; the framework will not provide it.

---

## Step definitions

**Before** (Serenity, `@Steps` style):

```java
public class LoginStepDefinitions {

    @Steps
    LoginSteps login;

    @Given("the user is on the login page")
    public void onLoginPage() {
        login.opensLoginPage();
    }

    @When("they login as {string} with password {string}")
    public void loginAs(String username, String password) {
        login.withCredentials(username, password);
    }
}
```

**After** (Selenium Boot): the step class extends `BaseCucumberSteps` and talks to a page object directly.

```java
public class LoginSteps extends BaseCucumberSteps {

    private LoginPage loginPage;

    @Given("the user is on the login page")
    public void onLoginPage() {
        open();                                  // navigates to execution.baseUrl
        loginPage = new LoginPage(getDriver());
    }

    @When("they login as {string} with password {string}")
    public void loginAs(String username, String password) {
        loginPage.login(username, password);
    }
}
```

`BaseCucumberSteps` gives you `getDriver()`, `getWait()`, `open()`, `open(path)`, `$()`, `assertThat()` and `getScenario()`. See [BDD / Cucumber](/docs/cucumber).

---

## Mapping the concepts

| Serenity | Selenium Boot |
|---|---|
| `@Steps` class | A page object extending [`BasePage`](/docs/guides/base-page) |
| Screenplay `Task` | A method on a page object |
| Screenplay `Question` | A page-object method that returns a value, asserted with `assertThat(...)` |
| `Actor` with abilities | Not provided. The thread's driver is available through `getDriver()`. |
| `@Managed WebDriver driver` | `getDriver()` |
| `Serenity.setSessionVariable(..)` / `sessionVariableCalled(..)` | [`ScenarioContext`](/docs/guides/scenario-context): `set`, `get`, `has`, `remove` |
| `@Step("Open the login page")` | `StepLogger.step("Open the login page")`. See [Step Logging](/docs/guides/step-logging). |
| `serenity.conf` `webdriver.base.url` | `execution.baseUrl` |
| `serenity.conf` `headless.mode` | `browser.headless` |
| `@WithTag` / Cucumber tags | Cucumber tags, unchanged. Filter with `-Dcucumber.filter.tags="@smoke"`. |

Gherkin step names already stream into the report timeline through `CucumberStepLogger`, so you do not annotate every method to get a readable report.

### Sharing state in step definitions

`ScenarioContext` is backed by a thread-local store that the framework clears after each scenario, so values do not leak between scenarios in parallel runs.

```java
new ScenarioContext().set("orderId", orderId);
String id = new ScenarioContext().get("orderId");
```

---

## Page objects

Serenity page objects extend `PageObject` and use `@FindBy` or `$(...)`. In Selenium Boot, extend `BasePage` and hold `By` constants:

```java
public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT   = By.id("submit");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(SUBMIT);
    }
}
```

Actions wait for the element to be ready, so there are no explicit waits in the page object. For accessibility-first lookups, see [Accessibility-First Locators](/docs/guides/semantic-locators).

---

## Assertions

Serenity commonly pairs steps with AssertJ or Hamcrest. Those still work in Selenium Boot. For element state, the built-in `assertThat` retries until the condition holds or `timeouts.explicit` elapses:

```java
assertThat(By.id("dashboard")).isVisible();
assertThat($(".cart-count")).hasText("3");
assertThat($(".item")).count(5);
```

---

## Configuration

```yaml title="selenium-boot.yml"
browser:
  name: chrome
  headless: true
execution:
  baseUrl: https://app.example.com
  parallel: none
retry:
  enabled: true
  maxAttempts: 1
```

Scenario retry is built in. Tag a scenario `@retryable` or `@retryable=2`, or enable `retry` globally; a failed scenario reruns from step 1 with a fresh driver. See [BDD / Cucumber](/docs/cucumber#retry).

---

## Reporting: what changes

This is the biggest behavioural difference, so it deserves care.

- **Serenity** builds a requirements-oriented "living documentation" site: results are organised by capability, feature and story, with outcome tallies across that hierarchy and per-step screenshots.
- **Selenium Boot** produces an [HTML report](/docs/reporting/html-report) organised by test and scenario: a step timeline per test, screenshots on failure, trends across runs, and JUnit XML for CI. It is not organised around a requirements hierarchy.

If requirement-level coverage reporting is something your stakeholders rely on, that does not carry over, and you should weigh it before migrating.

---

## Where Serenity does more

- **Screenplay.** A full Actor/Task/Question model with abilities. Selenium Boot has page objects and nothing above them.
- **Living documentation.** Requirements-level reporting, as above.
- **Built-in REST support.** Serenity integrates REST Assured into its reports. Selenium Boot has its own [API testing](/docs/guides/api-testing) client; it is a different API.

## Where Selenium Boot does more

- **Less to learn.** Cucumber steps, a page object and a `selenium-boot.yml` that is optional.
- **Selenium stays visible.** `getDriver()` is a real `WebDriver`.
- **Parallel runs, retries and cloud execution** are configuration. See [parallel execution](/docs/guides/parallel), [retry](/docs/guides/retry) and [cloud execution](/docs/cloud-execution).

---

## Next steps

- [BDD / Cucumber](/docs/cucumber): runner, glue and tags
- [BasePage](/docs/guides/base-page)
- [Migrate from Selenium + TestNG](/docs/migration/from-selenium-testng)
- [Coming from Selenide](/docs/migration/from-selenide)
