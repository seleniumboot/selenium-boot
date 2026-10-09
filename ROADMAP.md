# Selenium Boot – Roadmap v1.0

This document outlines the planned evolution of Selenium Boot from MVP to a stable, extensible automation framework.

The roadmap is intentionally opinionated and incremental. Each phase focuses on delivering production value before expanding scope.

> **Contributors, start here.** Phases 0–5 below are complete (the framework is past v1.0). The list
> immediately below is where active work and **open contribution opportunities** live today. The
> [issue tracker](https://github.com/seleniumboot/selenium-boot/issues) is the source of truth for what's
> actionable right now — this document is the higher-level picture.

---

## Post-1.0 — current focus & where help is welcome

Items tagged **`good first issue`** or **`help wanted`** are open for contribution. Read
[CONTRIBUTING.md](CONTRIBUTING.md), comment on the issue to claim it, then open a PR against `master`.

### Open issues to pick up

Every item below is a real, scoped issue with acceptance criteria. Comment to claim one.

**Documentation** (most are `good first issue`; docs live in `docs-site/docs/`)

- [#122](https://github.com/seleniumboot/selenium-boot/issues/122) Recipe: JavaScript alerts and confirm dialogs
- [#123](https://github.com/seleniumboot/selenium-boot/issues/123) Recipe: read and assert on table rows with `Locator.rows()`
- [#124](https://github.com/seleniumboot/selenium-boot/issues/124) Recipe: scroll to load more (infinite scroll)
- [#125](https://github.com/seleniumboot/selenium-boot/issues/125) Recipe: log in once with OAuth/SSO and reuse the session `help wanted`
- [#126](https://github.com/seleniumboot/selenium-boot/issues/126) Migration guide: coming from Selenide `help wanted`
- [#127](https://github.com/seleniumboot/selenium-boot/issues/127) Migration guide: coming from Serenity BDD `help wanted`
- [#128](https://github.com/seleniumboot/selenium-boot/issues/128) Why accessibility-first locators? / Why WaitEngine?
- [#129](https://github.com/seleniumboot/selenium-boot/issues/129) Document the Trace Viewer (no guide page yet)

**Design discussion**

- [#130](https://github.com/seleniumboot/selenium-boot/issues/130) What would make the Trace Viewer something teams share? Comment with a use case before any code.

### Already done (don't duplicate)

Per-page SEO descriptions · Why pages (Selenium Boot / plain Selenium / Playwright) · six recipes (upload, download PDF, iframes, Shadow DOM, drag & drop, new windows) · migration guides (Selenium+TestNG, WebDriverManager, Playwright) · homepage before/after · Edge and Safari local browsers.

### Ongoing

- More built-in `WaitEngine` conditions requested by users. `help wanted`
- Grow unit-test coverage for untested code paths. `good first issue`
- **seleniumboot-mcp** — keep MCP codegen output framework-native and accessibility-first as the API evolves. (See the [MCP repo](https://github.com/seleniumboot/selenium-mcp).)
- Keep the consumer sample project (`selenium-boot-test`) in step with new features.

> Don't see what you want to work on? Open a
> [Discussion](https://github.com/seleniumboot/selenium-boot/discussions) — ideas that fit the
> philosophy are welcome, and we'll turn agreed ones into issues.

---

## Guiding Roadmap Principles

- Deliver usable value early
- Stabilize before adding features
- Avoid speculative abstractions
- Optimize for real enterprise usage
- Prefer extensibility over monolithic growth

---

## Phase 0 – Foundation

**Status:** Complete
**Goal:** Establish core vision, scope, and structure

### Deliverables
- Project vision and positioning
- Opinionated design principles
- Initial repository structure
- Public roadmap and documentation baseline

---

## Phase 1 – MVP Core (v0.1)

**Status:** Complete — released as v0.1.0
**Goal:** Enable teams to run Selenium tests with minimal setup

### Features
- Java + Selenium + TestNG integration
- Opinionated project structure
- Automatic WebDriver management
- Centralized test lifecycle management
- Smart explicit waits with safe defaults
- Retry mechanism for flaky interactions
- Parallel execution enabled by default
- Single YAML-based configuration file
- Clean HTML execution report
- One-command execution via Maven

### Non-Goals
- Cross-framework support
- Plugin system
- Advanced reporting analytics

---

## Phase 2 – Stability & Observability (v0.2)

**Status:** Complete — released as v0.2.0
**Goal:** Improve reliability and execution transparency

### Features
- Enhanced retry intelligence (action-level vs test-level)
- Screenshot and page source capture on failure
- Execution summary with flaky test detection
- Execution timing and performance metrics
- Environment-aware configuration profiles
- Improved logging structure

---

## Phase 3 – Extensibility Layer (v0.3)

**Status:** Complete — releasing as v0.3.0
**Goal:** Allow controlled customization without breaking conventions

### Features
- ✅ Plugin-style extension points (`SeleniumBootPlugin` + `PluginRegistry`)
- ✅ Custom driver providers (`NamedDriverProvider` + `DriverProviderRegistry`)
- ✅ Custom reporting adapters (`ReportAdapter` + `ReportAdapterRegistry`)
- ✅ Hook system for execution lifecycle events (`ExecutionHook` + `HookRegistry`)
- ✅ Framework-safe overrides for defaults (`SeleniumBootDefaults`)

---

## Phase 4 – CI/CD & Enterprise Readiness (v0.4)

**Status:** Complete — releasing as v0.4.0
**Goal:** Seamless integration into enterprise pipelines

### Features
- ✅ CI-friendly execution modes (`CiEnvironmentDetector` — GitHub Actions, Jenkins, CircleCI, GitLab CI, Travis, TeamCity, Bitbucket)
- ✅ Parallel execution tuning for CI environments (thread count auto-derived from CPU cores)
- ✅ Machine-readable execution outputs (`JUnitXmlReporter` → `target/surefire-reports/TEST-SeleniumBoot.xml`)
- ✅ Build failure strategies and thresholds (`BuildThresholdEnforcer` — pass rate gate, flaky test gate)
- ✅ Docker-friendly execution support (`--no-sandbox`, `--disable-dev-shm-usage` auto-applied in containers)
- ✅ Sample CI templates (`.github/workflows/selenium-boot.yml`, `ci/Jenkinsfile`)

---

## Phase 5 – Ecosystem & Community (v1.0)

**Status:** Complete
**Goal:** Establish Selenium Boot as a stable ecosystem

### Features
- ✅ Official documentation website — live at https://seleniumboot.github.io/selenium-boot/
- ~~Sample reference projects~~ — replaced by the consumer test project at https://github.com/seleniumboot/selenium-boot-test
- ✅ Community contribution guidelines — see CONTRIBUTING.md
- ✅ Versioned plugin ecosystem — `FrameworkVersion`, `minFrameworkVersion()`, `IncompatiblePluginException`
- ✅ Backward compatibility guarantees — `@SeleniumBootApi` annotation, policy in CONTRIBUTING.md

---

## Roadmap Disclaimer

This roadmap represents current intent, not a fixed contract.

Priorities may shift based on:
- Community feedback
- Real-world adoption challenges
- Stability and maintenance considerations

---

## Contribution Alignment

All contributions should align with:
- The current roadmap phase
- The opinionated nature of the framework
- Long-term maintainability goals

Features that significantly increase complexity without clear value may be declined.

---

## Versioning Strategy (Planned)

- Pre-1.0 releases may introduce breaking changes
- Post-1.0 releases will follow semantic versioning
- Stability and predictability are prioritized over rapid feature growth
