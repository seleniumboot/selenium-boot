---
description: "Quarantine flaky Selenium tests: mark known-broken tests so the framework skips them automatically, without deleting or commenting out code."
id: quarantine
title: Test Quarantine
sidebar_position: 15
---

# Test Quarantine

Test quarantine lets you mark known-flaky or temporarily broken tests so the framework skips them automatically — without deleting or commenting out the test code, and without relying on any CI artifact history.

---

## How it works

Create a file called `selenium-quarantine.yml` in your **project root** (next to `pom.xml`). List the tests you want to skip. Commit it to version control. Every CI run — even a fresh clone — will skip them.

```yaml title="selenium-quarantine.yml"
quarantine:
  # Skip a specific test method
  - com.example.tests.LoginTest#loginWithExpiredSession

  # Skip an entire class (all methods)
  - com.example.tests.PaymentTest

  # Include an optional reason — shown in the skip message and logs
  - test: com.example.tests.SearchTest#searchWithSpecialChars
    reason: "Unicode handling broken — JIRA-1234"
```

That's it. No config changes, no annotations, no history required.

---

## Why a committed file?

CI pipelines clone the repo fresh on every run — there is no persistent `target/` directory between runs. Any solution that tracks "N consecutive failures" locally would break immediately in CI.

A committed YAML file solves this cleanly:

| Concern | Answer |
|---|---|
| Survives fresh clones | ✅ — it's in git |
| No external services needed | ✅ — just a YAML file |
| Auditable | ✅ — changes go through PRs |
| Works with all execution modes | ✅ — local, remote, BrowserStack, Sauce Labs |
| Works across team members | ✅ — everyone gets the same list |

---

## File format

### Plain string (simple)

```yaml
quarantine:
  - com.example.tests.LoginTest#loginTest          # specific method
  - com.example.tests.CheckoutTest                 # entire class
```

### Structured (with reason)

```yaml
quarantine:
  - test: com.example.tests.LoginTest#loginTest
    reason: "Token refresh race condition — JIRA-789"
```

Both formats can coexist in the same file.

### Identifier format

| Test type | Format | Example |
|---|---|---|
| TestNG method | `FullyQualifiedClass#methodName` | `com.example.LoginTest#loginTest` |
| JUnit 5 method | `FullyQualifiedClass#methodName` | `com.example.LoginTest#loginTest` |
| Entire class | `FullyQualifiedClass` | `com.example.LoginTest` |
| Cucumber scenario | `@quarantine` tag in `.feature` file | *(see below)* |

:::tip Finding the fully-qualified class name
It is the package + class name exactly as it appears in the `package` declaration.  
`package com.example.tests;` + `class LoginTest` → `com.example.tests.LoginTest`
:::

---

## Cucumber

Cucumber quarantine works **two ways** — use whichever fits the situation, or combine both.

---

### Method 1 — Tag in the feature file

Add the `@quarantine` tag directly to the scenario. No YAML entry needed.

```gherkin title="login.feature"
Feature: Login

  @quarantine
  Scenario: Login with expired session
    Given I have an expired session token
    When I try to log in
    Then I should see an error

  Scenario: Successful login
    Given I have valid credentials
    When I log in
    Then I should see the dashboard
```

Best for: single scenarios, when the owning team manages the feature file.

---

### Method 2 — Entries in `selenium-quarantine.yml`

Three YAML entry formats are supported for Cucumber:

#### By Cucumber tag — bulk quarantine across many features at once

Any scenario that carries the tag is skipped, regardless of which feature file it lives in.

```yaml title="selenium-quarantine.yml"
quarantine:
  - "@smoke"                         # skips every scenario tagged @smoke
  - test: "@regression"
    reason: "Payment refactor broke regression suite — JIRA-567"
```

Best for: quarantining a whole test category (a tag group) in one line.

#### By feature file — quarantine every scenario in a file

```yaml
quarantine:
  - login.feature                    # filename only
  - features/payment.feature         # relative path also works
  - test: features/checkout.feature
    reason: "Checkout service unavailable — JIRA-456"
```

The path is matched against the end of the scenario's URI, so both `login.feature` and
`features/login.feature` match `classpath:src/test/resources/features/login.feature`.

Best for: temporarily disabling an entire feature area without touching any `.feature` file.

#### By feature file + scenario name — specific scenario, no file edit needed

```yaml
quarantine:
  - "checkout.feature#Checkout with 3D Secure"
  - test: "login.feature#Login with expired token"
    reason: "Token refresh race condition — JIRA-789"
```

Format: `filename.feature#Exact scenario name` (case-insensitive name match).

Best for: quarantining one specific scenario when you can't or don't want to edit the feature file
(e.g. the file is shared across teams, or managed in a different repo).

---

### Comparison

| Method | Edits feature file? | Bulk support | Needs YAML? |
|---|---|---|---|
| `@quarantine` tag in scenario | Yes | No — one tag per scenario | No |
| `"@tag"` in YAML | No | Yes — one line per tag group | Yes |
| `"feature.file"` in YAML | No | Yes — entire file | Yes |
| `"feature.file#Scenario name"` in YAML | No | No — one scenario | Yes |

---

### Configuring the tag name

```yaml title="selenium-boot.yml"
quarantine:
  cucumberTag: quarantine   # default — change if this conflicts with your tag conventions
```

---

## File resolution order

The framework looks for the quarantine file in this priority order:

1. **System property** — `-Dselenium.boot.quarantine=/path/to/custom-quarantine.yml`
2. **Working directory** — `./selenium-quarantine.yml` (next to `pom.xml`)
3. **Classpath** — `src/test/resources/selenium-quarantine.yml`

If no file is found, quarantine is silently disabled (no error, no warning).

---

## Config

```yaml title="selenium-boot.yml"
quarantine:
  enabled: true           # set to false to temporarily disable without deleting the file
  cucumberTag: quarantine # Cucumber tag name (without the @ prefix)
```

`enabled: true` is the default — if the file exists, it is always applied.

---

## Disabling quarantine temporarily

To run the full suite including quarantined tests (e.g. to verify a fix):

```bash
# Option 1: system property override pointing to an empty file
mvn test -Dselenium.boot.quarantine=/dev/null

# Option 2: disable via config property  
mvn test -Dselenium.boot.config=config/no-quarantine.yml
```

Or set `quarantine.enabled: false` in a profile-specific YAML:

```bash
mvn test -Dselenium.boot.profile=full-run
```

```yaml title="selenium-boot-full-run.yml"
quarantine:
  enabled: false
```

---

## Workflow

1. A test fails intermittently in CI → file a ticket
2. Add the test to `selenium-quarantine.yml` with the ticket number as reason
3. Commit and push → CI stops failing on that test
4. Fix the root cause → remove the entry → verify in CI → close the ticket

Quarantine is a **temporary holding area**, not a permanent graveyard.

---

## What happens to a quarantined test

- Status recorded as **SKIPPED** in the HTML report and JUnit XML
- Skip message: `[Quarantined] com.example.LoginTest#loginTest — <reason>`
- No browser session is created — the test costs nothing to skip
- Counted in the skip total; does not affect pass rate or flakiness scores
