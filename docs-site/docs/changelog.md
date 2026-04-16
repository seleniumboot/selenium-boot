---
id: changelog
title: Changelog
sidebar_position: 99
---

# Changelog

All notable changes to Selenium Boot are documented here.

---

## [1.6.0] — 2026-04-16

### Added
- **Visual regression testing** — `VisualAssert.assertScreenshot(name)` pixel-by-pixel screenshot comparison; baseline auto-created on first run; diff image written to `target/visual-diffs/`; `VisualTolerance.of(n)` for configurable pixel-difference tolerance; `-DupdateBaselines=true` system property forces baseline regeneration; configurable dirs via `visual.baselineDir` / `visual.diffDir` in `selenium-boot.yml`
- **Mobile device emulation** — `DeviceEmulator.emulate("iPhone 14")` / `emulateDevice()` + `resetDevice()` in `BasePage`/`BaseTest`; full CDP emulation on Chrome/Edge (viewport, device scale factor, user-agent); window-resize + JS UA override fallback on Firefox; 6 built-in profiles: iPhone 14, iPhone SE, Pixel 7, Galaxy S23, iPad, iPad Pro 12; register custom profiles via `DeviceProfiles.register()`
- **Clipboard helpers** — `ClipboardHelper.write()` / `read()` / `clear()` backed by a reliable JS global store (`window.__seleniumBootClipboard`); async native clipboard attempted best-effort
- **GeoLocation mock** — `GeoLocation.set(lat, lon)` / `clear()`; CDP `Emulation.setGeolocationOverride` on Chrome/Edge; `navigator.geolocation` JS override fallback on Firefox
- **Network interception** — `NetworkMock.stub(urlPattern)` with fluent `StubBuilder`; glob patterns (`**/api/**`); configurable response body, content-type, status code, and delay; auto-cleared after each test
- **Browser storage helpers** — `StorageHelper.localStorage()`, `sessionStorage()`, `cookies()` — read/write/clear browser storage from tests without JS boilerplate
- **Fluent Locator API** — `$(css)` / `$(By)` returning a chainable `Locator`; methods: `filter()`, `withText()`, `within()`, `nth()`; auto-wait terminal actions: `click()`, `type()`, `getText()`, `isVisible()`, `count()`, `element()`, `elements()`
- **Web-First Assertions** — `assertThat(By)` / `assertThat(Locator)` returning `LocatorAssert`; auto-retrying assertions: `isVisible()`, `isHidden()`, `isEnabled()`, `hasText()`, `containsText()`, `hasValue()`, `hasAttribute()`, `hasClass()`, `count()`

---

## [1.3.0] — 2026-04-07

### Added
- **Shadow DOM helpers** — `ShadowDom` utility + 7 `BasePage` protected methods (`shadowFind`, `shadowFindAll`, `shadowClick`, `shadowType`, `shadowGetText`, `shadowPierce`, `shadowExists`)
- **Alert handling fix** — set `unhandledPromptBehavior: ignore` on all driver providers so native alerts stay open; `BasePage.getAndAcceptAlert()` convenience method
- **Component-aware waits** — `WaitEngine.waitForAngular()` (Angular 2+ testability API + AngularJS 1.x fallback) and `WaitEngine.waitForReactHydration()` (React 18/17/16 fiber detection, Next.js aware)
- **Enhanced HTML report** — pass rate gauge card, donut chart (Chart.js), retry badges, expandable inline error message + stack trace per failed row, filter buttons (`All / Passed / Failed / Skipped / Flaky`), text search, dark mode toggle, slowest-5 tests section
- **JUnit XML error details** — `<failure message>` and element text now contain the actual assertion message and full stack trace instead of a generic placeholder
- **Allure adapter** — opt-in Allure 2 result file generation; set `reporting.allureEnabled: true` in `selenium-boot.yml`; produces `target/allure-results/{uuid}-result.json` per test
- **Slack / Teams notifications** — configure `notifications.slack.webhookUrl` and/or `notifications.teams.webhookUrl`; post-suite summary sent automatically; `notifyOnFailureOnly` option
- **`@DependsOnApi`** — method- or class-level annotation; skips test before browser opens if the specified HTTP endpoint is unreachable; repeatable (multiple URLs, all must be up); result cached per suite to avoid redundant probes

---

## [1.1.1] — 2026-03-28

### Added
- **Schema validation** — `ApiResponse.assertSchema("schemas/user.json")` validates response body against a JSON Schema (Draft-07); requires `com.networknt:json-schema-validator:1.4.3` as consumer dependency
- **`@UseAuth` annotation** — apply a named auth strategy from `api.auth` config block to any test method/class
- **`ApiAuth.oauth2()`** — OAuth2 client credentials flow; token fetched on first use and cached until expiry
- **`ApiClient.setGlobalAuth()` / `clearGlobalAuth()`** — suite-level auth set once, applied to every request automatically; cleared by framework after each test

---

## [1.1.0] — 2026-03-25

### Added
- **`BaseApiTest`** — pure API test base class; no browser started; full framework lifecycle (reporting, `@TestData`, retry, CI gates)
- **`ApiClient`** — fluent HTTP client backed by Java's built-in `HttpClient`; `GET`, `POST`, `PUT`, `PATCH`, `DELETE`; per-request auth; auto step-logging
- **`ApiResponse`** — JSONPath extraction (`$.user.id`), `asObject(Class)`, fluent assertions (`assertStatus`, `assertJson`, `assertBodyContains`)
- **`ApiAuth`** — `bearerToken(token)`, `basicAuth(user, pass)`
- **`ScenarioContext`** — thread-local in-test store; `ctx().set/get`; auto-cleared after each test
- **`SuiteContext`** — global thread-safe store for cross-test state sharing; `suiteCtx().set/get`
- **`apiClient()`, `ctx()`, `suiteCtx()`** added to `BaseTest` for hybrid UI+API tests

---

## [0.10.0] — 2026-03-22

### Added
- **`@TestData`** — declarative test data injection via annotation; loads `.json`, `.yml`, `.yaml` from `src/test/resources/testdata/`; env-specific override when `-Denv=<profile>` is set; `getTestData()` returns `Map<String, Object>` in `BaseTest`
- **Browser matrix** — `browser.matrix: [chrome, firefox, edge]` in YAML runs all tests on all browsers in one invocation; `Browser` column added to HTML report; per-browser `TEST-selenium-boot-<browser>.xml` for Jenkins matrix view
- **`SessionCache`** — global (cross-thread) authenticated session store; `store("name")` captures cookies + localStorage; `restore("name")` applies them into the current driver and refreshes; `invalidate()` / `clear()` for teardown
- **SoftAssert** — `softAssert().that(condition, "message")` collects assertion failures without throwing; framework flushes at `onTestSuccess`; each failure logged as `FAIL` step entry; single screenshot at flush time; test marked `FAILED` with combined message

---

## [0.9.6] — 2026-03-21

### Added
- **DownloadManager browser auto-configuration** — Chrome sets `download.default_directory` via experimental prefs; Firefox sets `FirefoxProfile` download preferences; both skipped when `execution.mode: remote`
- **File upload helper** — `BasePage.upload(By, String)` resolves absolute → classpath → project-root; CI-safe absolute path sent to Selenium

---

## [0.9.5] — 2026-03-21

### Added
- **ConsoleErrorCollector auto-integration** — JS shim auto-injected on every `open()` call when `browser.captureConsoleErrors: true`; errors auto-collected at test end as `WARN` step entries in the HTML report
- **`failOnConsoleErrors` enforcement** — passing test with JS errors is marked FAILED when `browser.failOnConsoleErrors: true`
- **`StepStatus.WARN`** — new step status for JS error entries

---

## [0.9.4] — 2026-03-20

### Added
- **iFrame helpers expanded** — `withinFrameName(String nameOrId, Runnable action)` added to `BasePage`
- **Nested frame support** — `withinFrame`, `withinFrameIndex`, `withinFrameName` now use a thread-local depth counter; inner calls restore to `parentFrame()`, the outermost call restores to `defaultContent()`

---

## [0.9.3] — 2026-03-20

### Fixed
- **Alert methods use `this.driver`** — `acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert` in `BasePage` now build their `WebDriverWait` from `this.driver` (the driver passed into the page object constructor) instead of `WaitEngine` / `DriverManager.getDriver()`, eliminating the driver-mismatch that caused `NoAlertPresentException` in precondition context
- **`@PreCondition` error message** — `PreConditionRunner` now unwraps `InvocationTargetException` to expose the real cause, so the failure message is meaningful instead of showing "null"

---

## [0.9.2] — 2026-03-20

### Fixed
- **`@PreCondition` failure no longer triggers retry** — `TestExecutionListener` now catches precondition exceptions and re-throws as `SkipException`; the test is marked SKIPPED (not FAILED), the retry analyzer is not called, and no second browser is opened
- **`maxAttempts: 0` now respected in YAML** — `SeleniumBootConfig.Retry.maxAttempts` changed from `int` (default `1`) to nullable `Integer`; the defaults loader now only applies a programmatic override when the value was not set at all (was `null`), not when explicitly set to `0`

---

## [0.9.1] — 2026-03-19

### Fixed
- **Alert helpers now wait** — `acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert` in `BasePage` use `WaitEngine.waitForAlert()` instead of raw `driver.switchTo().alert()`, preventing `NoAlertPresentException` on slow pages

### Added
- **`WaitEngine.waitForAlert()`** — explicit wait for browser alert presence
- **`BasePage.smartFind(By primary, By... fallbacks)`** — convenience wrapper around `SmartLocator.find()` for use inside page objects without passing the driver manually

---

## [0.9.0] — 2026-03-18

### Added
- **`BasePage` expanded** — dropdowns (`selectByText`, `selectByValue`, `selectByIndex`, `getSelectedOption`), alerts (`acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert`), mouse actions (`hover`, `doubleClick`, `rightClick`), scroll (`scrollTo`, `scrollToTop`, `scrollToBottom`), JS fallbacks (`jsClick`, `jsType`)

---

## [0.8.0] — 2026-03-17

### Added
- **`BasePage`** — page object base class with `click`, `type`, `getText`, `getAttribute`, `isDisplayed`
- **iFrame helpers** — `withinFrame(By, Runnable)` and `withinFrameIndex(int, Runnable)` in `BasePage`
- **File upload helper** — `upload(By, String)` in `BasePage`, resolves classpath and relative paths
- **`SmartLocator`** — tries multiple locator strategies in order, returns first visible element
- **`DownloadManager`** — `waitForFile`, `waitForAnyFile`, `clearDownloads` with partial-download detection
- **`ConsoleErrorCollector`** — JS console error capture via WebDriver logs (Chrome) or injected shim (Firefox)
- **`@PreCondition`** — session-aware pre-conditions with automatic cookie + localStorage caching
- **`@ConditionProvider`** — marks provider methods in `BaseConditions` subclasses
- **`BaseConditions`** — base class for condition providers, gives `getDriver()`, `open()`, `click()`, `type()`
- **`@SeleniumBootApi`** — annotation marking stable public API with `since` version
- **`FrameworkVersion`** — runtime version access and `requireAtLeast()` compatibility check
- **`IncompatiblePluginException`** — thrown when plugin version requirements are not met
- **`minFrameworkVersion()`** — new method on `SeleniumBootPlugin` for version compatibility declarations
- **Config additions** — `browser.downloadDir`, `browser.captureConsoleErrors`, `browser.failOnConsoleErrors`

---

## [0.7.0] — 2026-03-16

### Added
- **Browser lifecycle control** — `browser.lifecycle: per-test | per-suite` setting
- **Per-suite driver management** — browser stays open across tests when `per-suite` is configured
- **Step logging** — `StepLogger.step()` API for named test steps with optional screenshots and status badges
- **Step timeline** — Step-by-step execution timeline in the HTML report detail panel
- **Tabbed HTML report** — Left sidebar navigation with Dashboard, Test Cases, and Failures tabs
- **Failures tab** — Pre-expanded failure details for faster debugging
- **Inline step screenshots** — Base64-embedded step screenshots with lightbox on click

### Changed
- HTML report overhauled with tab-based layout and collapsible test rows
- Screenshot lightbox now opens full-size correctly

---

## [0.6.0] — 2025-12-01

### Added
- **Advanced HTML reporting** — Dashboard with metrics cards, test case table, retry summary
- **Donut chart** — Pass/fail/skip distribution chart on Dashboard tab
- **Slowest tests** — Top-5 slowest tests ranked by duration
- **Pass rate gauge** — Colour-coded pass rate percentage card
- **Retry badge** — `↻ Nx` badge on retried tests in the table

### Changed
- Screenshots embedded as Base64 — report is now a single self-contained file

---

## [0.5.0] — 2025-10-15

### Added
- **Retry support** — `retry.enabled` + `retry.maxAttempts` in `selenium-boot.yml`
- **`@Retryable` annotation** — per-method retry override
- **RetryAnnotationTransformer** — auto-registered via Java SPI, zero config
- **Retry metrics** — retry counts tracked in `ExecutionMetrics` and exported to JSON

---

## [0.4.0] — 2025-08-20

### Added
- **`WaitEngine`** — fluent explicit wait API (`waitForVisible`, `waitForClickable`, `waitForText`, etc.)
- **Timeout override** — `getWait(seconds)` for per-call timeout override
- **`waitForStaleness`** — wait for DOM element replacement after AJAX reload

---

## [0.3.0] — 2025-06-10

### Added
- **`BasePage`** — page object base class with `click`, `type`, `getText`, `isDisplayed`, `getAttribute`
- **Parallel execution** — `parallel.enabled` + `parallel.threadCount` configuration
- **Session semaphore** — `browser.maxActiveSessions` cap on concurrent browser instances
- **JUnit XML reporter** — `target/surefire-reports/TEST-SeleniumBoot.xml`

---

## [0.2.0] — 2025-04-05

### Added
- **`BaseTest`** — test base class with `open()`, `open(path)`, `getDriver()`, `getWait()`
- **`SeleniumBootConfig`** — YAML configuration loader (`selenium-boot.yml`)
- **`DriverManager`** — ThreadLocal WebDriver lifecycle management
- **Automatic driver setup** — WebDriverManager integration, no manual driver downloads
- **Headless mode** — `browser.headless: true`
- **Basic HTML report** — pass/fail/skip counts and duration
- **Screenshot on failure** — automatic capture, embedded in report

---

## [0.1.0] — 2025-02-01

### Added
- Initial release
- Chrome and Firefox support
- Basic TestNG integration
- `selenium-boot.yml` configuration file discovery
