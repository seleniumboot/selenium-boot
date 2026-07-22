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
- **Convention over configuration** — `SeleniumBootDefaults` supplies fallbacks for optional fields, but `selenium-boot.yml` itself is **required**: `ConfigurationLoader` throws if the file is absent, and validates that `execution.mode`, `browser.name` (or `browser.matrix`), `timeouts.explicit` and `timeouts.pageLoad` are all present and positive.

### Public API Contract

Classes/methods annotated `@SeleniumBootApi` are the stable public surface. Avoid breaking changes to these without a version bump. Internal packages (classes not annotated) may change freely.

## Tests

Unit tests are in `src/test/java/com/seleniumboot/unit/` (39 test classes, 480 tests). They use **TestNG + Mockito**. Tests mock Selenium/browser interactions — no real browser is required to run the test suite.

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
| `README.md` | Maven dependency snippet + "Current release" line under `## Project Status` |
| `CHANGELOG.md` | New release entry at top |
| `docs-site/docs/getting-started.md` | Maven dependency snippet |
| `docs-site/docs/junit5.md` | Maven dependency snippet |
| `docs-site/docs/changelog.md` | New release entry at top |
| `docs-site/src/pages/index.js` | Maven dependency snippet in hero section |
| `PLAN.md` | Released versions table |

**Outside this repo** — the marketing site ships the version too, so a bump here without
a bump there advertises a stale release on seleniumboot.com:

| Repo / File | Location |
|---|---|
| `seleniumboot/website` → `src/data/content.ts` | `LATEST_VERSION` — feeds the copy-to-clipboard Maven snippet in the homepage CTA |

Also add a new changelog entry in `CHANGELOG.md` and `docs-site/docs/changelog.md` describing what changed. The full version history lives in `CHANGELOG.md` (extracted from the README); the README only keeps a one-line "Current release" pointer.

### Release checklist (every version bump)

1. Update all version references in the in-repo table above.
2. Add the `CHANGELOG.md` + `docs-site/docs/changelog.md` entries.
3. Commit and push.
4. **Tag the release commit and push the tag** — lightweight tag named `vX.Y.Z`
   on the commit that was published (not later CI/housekeeping commits):
   ```bash
   git tag v3.1.0 <release-commit>
   git push origin v3.1.0
   ```
   Verify afterwards with `git describe --tags` (HEAD should resolve to the new tag).
5. **Create the GitHub Release** from the tag — a pushed tag does **not** create a
   Release, so the repo's "Releases" page keeps showing the old version until you do:
   ```bash
   gh release create v3.1.0 --title "v3.1.0 — <summary>" --latest --notes "<changelog>"
   ```
   Verify with `gh release list` (newest version should show "Latest").
6. Publish to Maven Central (`mvn deploy`) and finish the manual publish in the
   Central Portal (see Publishing below).
7. **Last**, bump `LATEST_VERSION` in the `seleniumboot/website` repo and push to
   `main` (Actions deploys it). Do this *after* the artifact is live on Central —
   the homepage snippet is copy-to-clipboard, so bumping earlier hands visitors a
   `pom.xml` that won't resolve yet.

## Publishing

Maven Central publishing is handled by `central-publishing-maven-plugin` with GPG signing. The CI workflow (`.github/workflows/selenium-boot.yml`) runs `mvn test`. Publishing is a manual step.

GPG key and Maven Central credentials are configured in `~/.m2/settings.xml`. To publish:

```bash
mvn deploy
```

No extra flags needed — GPG signing and Central Portal upload are handled automatically via the configured settings.
