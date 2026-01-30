# Selenium Boot – Opinionated Project Structure

This document defines the standard, opinionated project structure enforced by Selenium Boot.

The structure is designed to promote consistency, readability, and long-term maintainability across automation projects.

---

## Design Goals

The project structure aims to:

- Reduce decision fatigue for teams
- Enforce separation of concerns
- Scale from small suites to enterprise test bases
- Support parallel execution safely
- Align with Maven and TestNG conventions

---

## Root Directory Layout

```
selenium-boot-project
├── pom.xml
├── selenium-boot.yml
├── README.md
├── docs/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com.yourorg.automation
│   │           └── framework/
│   └── test/
│       ├── java/
│       │   └── com.yourorg.automation
│       │       ├── tests/
│       │       ├── pages/
│       │       ├── flows/
│       │       └── utils/
│       └── resources/
│           └── test-data/
└── target/
```

---

## Key Files

### pom.xml
- Maven build and dependency configuration
- Selenium Boot starter dependency
- Test execution profiles

### selenium-boot.yml
- Single source of configuration
- Environment-specific overrides
- Execution, browser, and reporting settings

---

## Source Directories

### src/main/java

Reserved for:
- Custom framework extensions
- Hooks and listeners
- Enterprise-specific integrations

Test logic must not be placed here.

---

### src/test/java

#### tests/
- TestNG test classes
- Business-level test scenarios
- No WebDriver logic

#### pages/
- Page Object Model classes
- Encapsulated UI interactions
- No assertions

#### flows/
- High-level business workflows
- Reusable multi-page interactions
- Optional but recommended for large suites

#### utils/
- Test-only utilities
- Data helpers
- Non-framework logic

---

### src/test/resources

#### test-data/
- Externalized test data
- JSON, YAML, CSV formats
- Environment-agnostic

---

## Package Naming Conventions

- Base package must be consistent across project
- Clear separation between framework and tests
- Avoid deeply nested packages

Example:
```
com.company.project.tests
com.company.project.pages
com.company.project.flows
```

---

## Rules and Constraints

- Tests must not manage WebDriver lifecycle
- No static WebDriver references
- No implicit waits
- Page Objects must not contain assertions
- Configuration must come from selenium-boot.yml

Violations reduce framework stability and are discouraged.

---

## Scaling Considerations

- Modularize by feature for large test suites
- Use flows to reduce test duplication
- Keep test classes small and focused

---

## Summary

This opinionated project structure removes ambiguity and enforces discipline.
Teams adopting Selenium Boot should follow this layout to ensure consistency,
stability, and maintainability across automation projects.
