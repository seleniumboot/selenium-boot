---
description: "Push Selenium test results to TestRail and Xray automatically: one annotation, no extra reporting code in your test methods."
id: test-management
title: TestRail & Xray Integration
sidebar_position: 16
---

# TestRail & Xray Integration

Selenium Boot pushes test results to **TestRail** and/or **Xray** automatically — no extra code in your test methods beyond a single annotation.

---

## Quick Start

### 1 — Annotate your tests

```java
import com.seleniumboot.testmanagement.TestRailCase;
import com.seleniumboot.testmanagement.XrayTest;

public class LoginTest extends BaseTest {

    @Test
    @TestRailCase("C1234")
    @XrayTest("PROJ-99")
    public void validLogin() {
        open();
        $("input#email").type("admin@example.com");
        $("input#password").type("secret");
        $("button[type='submit']").click();
        assertThat(By.id("dashboard")).isVisible();
    }

    // Multiple IDs on one test
    @Test
    @TestRailCase({"C1234", "C5678"})
    @XrayTest({"PROJ-99", "PROJ-100"})
    public void checkoutFlow() { ... }
}
```

### 2 — Configure in `selenium-boot.yml`

```yaml
testmanagement:
  testrail:
    enabled: true
    url: https://yourcompany.testrail.io
    username: user@example.com
    apiKey: YOUR_API_KEY
    projectId: 1
    suiteId: 2
    runName: "Selenium Boot – CI run"

  xray:
    enabled: true
    mode: cloud
    clientId: YOUR_CLIENT_ID
    clientSecret: YOUR_CLIENT_SECRET
    projectKey: PROJ
```

That's it — run `mvn test` and results appear in both tools.

---

## TestRail

### Authentication

TestRail uses HTTP Basic authentication. The `apiKey` is the API key generated in **My Settings → API Keys** in your TestRail instance (not your login password).

### Run management

By default, Selenium Boot creates a new test run at suite start:

```yaml
testmanagement:
  testrail:
    autoCreateRun: true      # default — creates a fresh run each time
    runName: "Regression – ${BUILD_NUMBER}"
```

To post results into an **existing run**, disable auto-creation and provide the run ID:

```yaml
testmanagement:
  testrail:
    autoCreateRun: false
    runId: 42
```

### Status mapping

| Selenium Boot | TestRail |
|---|---|
| `PASSED`  | 1 — Passed |
| `FAILED`  | 5 — Failed |
| `SKIPPED` | 4 — Retest |

Failed tests include the exception message as the TestRail result comment, making root-cause triage faster.

### Case ID format

Both `"C1234"` and `"1234"` are accepted — the leading `C` is optional.

```java
@TestRailCase("C1234")   // ✓
@TestRailCase("1234")    // ✓ same case
```

---

## Xray

### Cloud mode (Jira Cloud)

```yaml
testmanagement:
  xray:
    enabled: true
    mode: cloud
    clientId: YOUR_CLIENT_ID
    clientSecret: YOUR_CLIENT_SECRET
    projectKey: PROJ
```

Selenium Boot obtains a JWT token from `https://xray.cloud.getxpecto.com/api/v2/authenticate` and imports results to the same host. Generate `clientId` / `clientSecret` in Jira → **Xray → API Keys**.

### Server / Data Center mode

```yaml
testmanagement:
  xray:
    enabled: true
    mode: server
    jiraUrl: https://jira.yourcompany.com
    username: automation-user
    password: ${JIRA_PASSWORD}      # supports env-var substitution
    projectKey: PROJ
```

Results are imported to `{jiraUrl}/rest/raven/1.0/import/execution`.

### Linking to a Test Plan

```yaml
testmanagement:
  xray:
    testPlanKey: PROJ-1      # links every execution to this plan
```

### Status mapping

| Selenium Boot | Xray |
|---|---|
| `PASSED`  | `PASS` |
| `FAILED`  | `FAIL` |
| `SKIPPED` | `TODO` |

### Batch import

Unlike TestRail (which pushes each result immediately), Xray results are **collected during the run** and imported as a single execution payload at suite end. This reduces API calls and keeps the Xray execution record coherent.

---

## Config Reference

```yaml
testmanagement:

  testrail:
    enabled: false              # true to activate
    url:                        # https://yourcompany.testrail.io
    username:                   # email or username
    apiKey:                     # API key from My Settings → API Keys
    projectId: 0                # TestRail project ID
    suiteId: 0                  # omit for single-suite projects
    runName: "Selenium Boot Run"
    autoCreateRun: true         # false → provide runId below
    runId: 0                    # used when autoCreateRun: false

  xray:
    enabled: false              # true to activate
    mode: cloud                 # "cloud" | "server"
    # Cloud:
    clientId:
    clientSecret:
    # Server/DC:
    jiraUrl:
    username:
    password:
    # Shared:
    projectKey:                 # e.g. "PROJ"
    testPlanKey:                # optional — links to an existing Test Plan
```

---

## Using with CI

Store credentials as CI secrets and pass them via environment variables:

```yaml
# selenium-boot.yml
testmanagement:
  testrail:
    enabled: true
    url: https://yourcompany.testrail.io
    username: ${TESTRAIL_USER}
    apiKey: ${TESTRAIL_KEY}
    projectId: 1
```

```yaml
# GitHub Actions
- name: Run tests
  env:
    TESTRAIL_USER: ${{ secrets.TESTRAIL_USER }}
    TESTRAIL_KEY:  ${{ secrets.TESTRAIL_KEY }}
  run: mvn test
```

Environment variable substitution (`${VAR_NAME}`) is resolved from `System.getenv()` then `System.getProperty()`.

---

## Class-Level Annotation

Apply the annotation at the class level to link every method in the class to the same TestRail case or Xray test key:

```java
@TestRailCase("C999")   // every method in this class reports to C999
@XrayTest("PROJ-500")
public class SmokeTests extends BaseTest {

    @Test
    public void homepageLoads() { ... }

    @Test
    public void loginPageLoads() { ... }
}
```

Method-level annotations take precedence over class-level annotations.
