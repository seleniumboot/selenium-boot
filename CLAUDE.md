# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Selenium Boot** is a Java library (JAR) published to Maven Central — an opinionated, zero-boilerplate Selenium automation framework inspired by Spring Boot. Users add it as a single dependency; the framework handles WebDriver lifecycle, waits, retries, reporting, and CI integration automatically.

- **GroupId / ArtifactId:** `io.github.seleniumboot / selenium-boot`
- **Java:** 17+ required
- **Build:** Maven 3.8+
- **Test Framework:** TestNG 7.9.0

## Common Commands

```bash
# Run all unit tests
mvn test

# Full build (compile + test + package)
mvn clean verify

# Install locally for testing with consumer projects
mvn clean install -DskipTests

# Run a single test class
mvn test -Dtest=ConfigurationLoaderTest

# Run a single test method
mvn test -Dtest=ConfigurationLoaderTest#testMethodName

# Run tests against a specific environment profile
mvn test -Denv=staging

# Docs site (Docusaurus) — from docs-site/
npm install
npm run start   # dev server
npm run build   # production build
```

No linter/formatter is enforced. Follow the style of existing code.

## Architecture

The framework is a **4-layer library**:

```
Consumer Test Code  (BaseTest / BasePage subclasses, user's test project)
       ↑
Selenium Boot Core  (lifecycle, driver mgmt, waits, retries, reporting)
       ↑
Infrastructure      (config loading, SPI plugin registry, listeners)
       ↑
Selenium WebDriver  (browser automation)
```

All source lives under `src/main/java/com/seleniumboot/`. Key packages:

| Package | Role |
|---|---|
| `test/` | `BaseTest`, `BasePage`, `SmartLocator` — user-facing base classes |
| `driver/` | `DriverManager` (ThreadLocal), provider registry, Chrome/Firefox/Remote providers |
| `config/` | `ConfigurationLoader` reads `selenium-boot.yml`; `SeleniumBootDefaults` supplies fallbacks |
| `lifecycle/` | `FrameworkBootstrap` — wires everything together at suite start |
| `listeners/` | TestNG `SuiteExecutionListener` / `TestExecutionListener`; `@Retryable` + `RetryListener` |
| `hooks/` | `HookRegistry` + `ExecutionHook` — plugin lifecycle callbacks |
| `precondition/` | `@PreCondition` / `@ConditionProvider` — session caching (cookies, localStorage) |
| `wait/` | `WaitEngine` — centralised explicit waits, no `Thread.sleep()` |
| `reporting/` | HTML report, JUnit XML, `ScreenshotManager`, adapter registry |
| `extension/` | `PluginRegistry` + SPI — custom drivers, report adapters, hooks |
| `ci/` | `CiEnvironmentDetector`, `BuildThresholdEnforcer` — pass-rate gates |
| `steps/` | `StepLogger`, `StepRecord` — named test steps with screenshots |
| `junit5/` | Optional JUnit 5 bridge (`BaseJUnit5Test`, `@EnableSeleniumBoot`) |

### Key Design Patterns

- **Registry + SPI** — `DriverProviderRegistry`, `PluginRegistry`, `ReportAdapterRegistry` use `ServiceLoader` for extensibility.
- **ThreadLocal driver isolation** — `DriverManager` stores `WebDriver` per thread; safe for parallel TestNG execution.
- **Template Method** — `BaseTest` / `BasePage` define framework-managed lifecycle; users override hook methods.
- **Convention over configuration** — `SeleniumBootDefaults` provides sensible defaults; `selenium-boot.yml` is optional.

### Public API Contract

Classes/methods annotated `@SeleniumBootApi` are the stable public surface. Avoid breaking changes to these without a version bump. Internal packages (classes not annotated) may change freely.

## Tests

Unit tests are in `src/test/java/com/seleniumboot/unit/` (12 test classes). They use **TestNG + Mockito**. Tests mock Selenium/browser interactions — no real browser is required to run the test suite.

## Configuration Reference

Consumer projects configure via `selenium-boot.yml` at the project root:

```yaml
execution:
  mode: local | remote
  baseUrl: https://example.com
  parallel: none | methods | classes
  threadCount: 4

browser:
  name: chrome | firefox
  headless: true | false
  lifecycle: per-test | per-suite

retry:
  enabled: true
  maxAttempts: 2

timeouts:
  explicit: 10      # seconds
  pageLoad: 30

ci:
  failOnPassRateBelow: 80
  maxFlakyTests: 3
```

## Version Bumps

When upgrading the project version, **always update all places the version is referenced**:

| File | Location |
|---|---|
| `pom.xml` | `<version>` tag |
| `README.md` | Maven dependency snippet + changelog entry |
| `docs-site/docs/getting-started.md` | Maven dependency snippet |
| `docs-site/docs/junit5.md` | Maven dependency snippet |
| `docs-site/docs/changelog.md` | New release entry at top |
| `PLAN.md` | Released versions table |

Also add a new changelog entry in `README.md` and `docs-site/docs/changelog.md` describing what changed.

## Publishing

Maven Central publishing is handled by `central-publishing-maven-plugin` with GPG signing. The CI workflow (`.github/workflows/selenium-boot.yml`) runs `mvn test`. Publishing is a manual step.

GPG key and Maven Central credentials are configured in `~/.m2/settings.xml`. To publish:

```bash
mvn deploy
```

No extra flags needed — GPG signing and Central Portal upload are handled automatically via the configured settings.
