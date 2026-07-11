---
description: "Selenium Boot release notes and version history: new features, fixes, and breaking changes across every release."
id: changelog
title: Changelog
sidebar_position: 99
---

# Changelog

All notable changes to Selenium Boot are documented here.

---

## [3.1.1] — 2026-06-26

### Fixed
- **Report overwrite with multiple test engines** — the metrics JSON, HTML report, and metrics history now honor the `seleniumboot.reports.dir` system property (default `target`). When a TestNG suite (Surefire) and JUnit 5 tests (Failsafe) run in the same build, point each engine's run at its own directory (e.g. `-Dseleniumboot.reports.dir=target/junit5`) so they no longer overwrite each other's HTML report. New `ReportPaths` helper centralizes path resolution.

## [3.1.0] — 2026-06-25

### Added
- **Accessibility-first locators** — Playwright-style semantic locators available on `BaseTest` and `BasePage`: `getByRole`, `getByText`, `getByLabel`, `getByPlaceholder`, `getByTestId`, `getByAltText`, `getByTitle`. They target the accessibility tree the user perceives rather than brittle CSS/DOM structure, so tests survive redesigns.
- **`getByRole(Role)`** — 38 WAI-ARIA roles, each matching implicit HTML elements (`<button>`, `<a href>`, `<h1>`…) and explicit `role="…"` attributes. Refine with `.withName("Submit")` (accessible-name match, following ARIA precedence: `aria-label` → `aria-labelledby` → associated `<label>` → text → `value`/`alt`/`title`) and `.withLevel(1)` (heading level).
- **Case-insensitive substring matching by default**, with `.exact()` opt-in. All locators flow through the existing auto-wait `Locator` chain — no `Thread.sleep`, no explicit waits.
- **`toBy()` escape hatch** — every semantic locator can return its synthesized Selenium `By` for interop with raw Selenium or `SmartLocator`.
- **Configurable test-id attribute** — `locators.testIdAttribute` in `selenium-boot.yml` (default `data-testid`).

---

## [3.0.0] — 2026-06-21

### Added
- **TestRail Integration** — `@TestRailCase("C1234")` on any test method (or class) pushes results to TestRail automatically; supports multiple IDs (`@TestRailCase({"C1234", "C5678"})`); creates a named run on suite start (`autoCreateRun: true`); maps PASSED→1, FAILED→5, SKIPPED→Retest(4); failure exception message is sent as the result comment
- **Xray Integration** — `@XrayTest("PROJ-123")` pushes results to Xray Cloud or Xray Server/DC; Cloud uses OAuth2 client credentials; Server uses HTTP Basic auth against Jira; results are batch-imported at suite end
- **Zero extra dependencies** — both clients use `java.net.http.HttpClient` (built into Java 17)
- **TestNG + JUnit 5** — same annotations work in both test frameworks; framework automatically detects and routes to the correct listener

### Config
```yaml
testmanagement:
  testrail:
    enabled: true
    url: https://yourcompany.testrail.io
    username: user@example.com
    apiKey: YOUR_API_KEY
    projectId: 1
    suiteId: 2            # optional — omit for single-suite projects
    runName: "Selenium Boot – CI run"
    autoCreateRun: true   # set false and provide runId to use an existing run

  xray:
    enabled: true
    mode: cloud           # "cloud" (Jira Cloud) or "server" (Server / Data Center)
    # Cloud fields:
    clientId: YOUR_CLIENT_ID
    clientSecret: YOUR_CLIENT_SECRET
    # Server/DC fields:
    # jiraUrl: https://jira.example.com
    # username: admin
    # password: secret
    projectKey: PROJ
    testPlanKey: PROJ-1   # optional — links the execution to a Test Plan
```

---

## [2.6.0] — 2026-06-20

### Added
- **Gradle Build Support** — `testImplementation 'io.github.seleniumboot:selenium-boot:2.6.0'` + `test { useTestNG() }` is the complete Gradle setup; full docs cover Groovy DSL, Kotlin DSL, JUnit 5 bridge, parallel execution, optional dependencies, and `./gradlew test` equivalents for all `mvn` commands
- **JUnit XML auto-detection** — `JUnitXmlReporter` now detects the active build tool at runtime: writes to `build/test-results/test/` (Gradle) when only a `build/` directory exists, or `target/surefire-reports/` (Maven) otherwise; override with `-Dseleniumboot.reports.dir=` system property
- **Cross-build-tool version reporting** — `FrameworkVersion.get()` now reads `Implementation-Version` from the JAR's `MANIFEST.MF` as the primary source (works with both Maven and Gradle); falls back to `META-INF/maven/.../pom.properties` (Maven-only) and then `"0.0.0"`; `maven-jar-plugin` configured with `addDefaultImplementationEntries: true` to populate the manifest on every Maven build

---

## [2.5.0] — 2026-06-20

### Added
- **Accessibility Assertions (axe-core)** — `accessibility()` in `BaseTest` and `BaseJUnit5Test` runs a full axe-core WCAG scan on the active page; axe-core 4.10.2 bundled in the JAR — no CDN, no extra Maven dependency required
- Fluent builder: `.withTags("wcag2a", "wcag21aa")` restricts rules to WCAG 2.1 AA; `.withLevel(Impact.SERIOUS)` filters violations by minimum severity; `.excluding("#cookie-banner")` skips known third-party elements; `.withContext("#main-form")` scopes the scan to a subtree
- `.run()` — asserts zero violations and throws a detailed `AssertionError` on failure, showing rule ID, severity (`CRITICAL`/`SERIOUS`/`MODERATE`/`MINOR`), fix guidance, element CSS selector path, and link to the axe-core docs for each failing node
- `.collect()` — returns raw `AccessibilityResult` for custom inspection without asserting; `result.violations()`, `result.violationsAtLevel(Impact.SERIOUS)`, `result.passCount()`
- `Impact` enum with ordering: `CRITICAL > SERIOUS > MODERATE > MINOR`; `Impact.fromString(str)` parses axe-core impact strings
- `AccessibilityResult`, `AccessibilityViolation`, `AccessibilityViolation.NodeDetail` all available via `accessibility().collect()` for custom reporting or soft assertions

---

## [2.4.0] — 2026-05-19

### Added
- **Performance Assertions (Core Web Vitals)** — `assertPerformance()` collects LCP, FCP, TTFB, CLS, DOM load, and page load from the active browser page using browser-native APIs (`window.performance.getEntriesByType()`); no extra dependency or proxy required
- Fluent assertion chain: `.lcp().isBelow(2500).fcp().isBelow(1800).ttfb().isBelow(600).cls().isBelow(0.1)` with colour-coded error messages showing actual vs threshold values
- `collectPerformance()` — raw `PerformanceMetrics` access for custom assertions or logging
- LCP/CLS available on Chrome/Edge only; assertions on unavailable metrics are silently skipped (not failed), enabling cross-browser test suites
- `performance.captureOnEveryTest: true` — auto-captures metrics after every passing test; ⚡ Performance strip with green/yellow/red chips shown in the HTML report test detail panel
- `PerformanceAssert`, `PerformanceMetrics`, `PerformanceCollector` all available via `clock()` pattern in `BaseTest` and `BaseJUnit5Test`

### Config
```yaml
performance:
  captureOnEveryTest: false   # show metrics in HTML report for every test
  lcpWarnMs:  2500
  fcpWarnMs:  1800
  ttfbWarnMs: 800
  clsWarn:    0.1
```

---

## [2.3.0] — 2026-05-17

### Added
- **Test Quarantine** — `selenium-quarantine.yml` in the project root lists tests to skip permanently; committed to version control so it survives fresh CI clones; supports TestNG, JUnit 5, and Cucumber; two entry formats: plain string (`com.example.LoginTest#method`) and structured with optional reason (`test: …\nreason: "JIRA-123"`)
- **Class-level quarantine** — a class-only entry (`com.example.PaymentTest`) skips every method in that class
- **Cucumber quarantine — two methods**: (1) add `@quarantine` tag to a scenario in the `.feature` file; (2) list entries in `selenium-quarantine.yml` using any of three formats: by Cucumber tag (`"@smoke"` — bulk across all features carrying that tag), by feature file (`login.feature` — all scenarios in the file), or by feature+name (`"login.feature#Login with expired session"` — specific scenario without editing the feature file)
- **`quarantine.enabled`** flag — set to `false` to temporarily run the full suite without removing entries from the file
- **File resolution** — system property `-Dselenium.boot.quarantine=`, working directory, classpath (in that order); missing file = silent no-op

### Config
```yaml
quarantine:
  enabled: true           # false = disable without editing the file
  cucumberTag: quarantine # Cucumber tag name (without @)
```

```yaml title="selenium-quarantine.yml"
quarantine:
  - com.example.tests.LoginTest#loginWithExpiredSession
  - com.example.tests.PaymentTest                        # entire class
  - test: com.example.tests.SearchTest#searchSpecial
    reason: "JIRA-1234 — Unicode handling broken"
```

---

## [2.2.0] — 2026-05-12

### Added
- **External `@TestData` sources** — `@TestData` now accepts `csv:`, `excel:`, and `db:` prefixes in addition to the existing JSON/YAML files; `sheet` attribute selects an Excel worksheet; `row` attribute picks the zero-based data row (header excluded); type coercion applied automatically (integers, doubles, booleans); Apache POI required for Excel (add `poi-ooxml:5.2.5` to your project, optional dep)
- **CSV source** — `@TestData("csv:testdata/logins.csv")` — RFC 4180 quoting support, built-in parser, no extra dependency
- **Excel source** — `@TestData(value = "excel:testdata/users.xlsx", sheet = "Login")` — reads XLSX via Apache POI; cell type mapping (numeric → `long`/`double`, date-formatted → ISO string, boolean → `Boolean`)
- **DB source** — `@TestData("db:SELECT username, password FROM test_users WHERE active=1")` — executes against the `database` config block; first result row loaded; participates in per-test connection lifecycle
- **`TestClock`** — `clock().set("2030-01-01T00:00:00Z")` injects a JS `Date` override into the browser; `clock().advance(Duration.ofDays(30))` fast-forwards relative to the current mock; `clock().reset()` restores real time; all three available via `clock()` in `BaseTest` and `BaseJUnit5Test`; auto-reset called automatically after every test (pass, fail, skip)
- `clock` config block: `clock.injectHeader` / `clock.headerName` for optional server-side date header propagation

### Config
```yaml
clock:
  injectHeader: false      # send X-Mock-Date header to server
  headerName: X-Mock-Date
```

---

## [2.1.0] — 2026-05-04

### Added
- **BrowserStack integration** — `execution.mode: browserstack`; `BrowserStackProvider` builds W3C `bstack:options` capabilities from YAML config; supports desktop (`os`, `osVersion`, `browser`, `browserVersion`) and mobile (`device`, `realMobile`); raw `bstack:options` overrides via `capabilities` map; zero test-code change — all framework features work identically
- **Sauce Labs integration** — `execution.mode: saucelabs`; `SauceLabsProvider` builds W3C `sauce:options` capabilities; three regions supported: `us-west-1`, `eu-central`, `apac-southeast`; raw `sauce:options` overrides via `capabilities` map
- **Cloud session URL in HTML report** — after driver creation on BrowserStack or Sauce Labs, the session dashboard URL is captured from the remote session ID and stored; HTML report shows a **"☁ View Session"** link in the test detail panel linking directly to the BrowserStack/Sauce video and logs
- `DriverManager.getCloudSessionUrl()` — public accessor for the current thread's cloud session URL; `null` when running locally or against a self-hosted grid

### Config
```yaml
execution:
  mode: browserstack   # or: saucelabs | remote | local

  browserstack:
    username:      ${BS_USER}
    accessKey:     ${BS_KEY}
    os:            Windows
    osVersion:     "11"
    browser:       chrome
    browserVersion: latest

  saucelabs:
    username:      ${SAUCE_USER}
    accessKey:     ${SAUCE_KEY}
    region:        us-west-1      # us-west-1 | eu-central | apac-southeast
    platformName:  "Windows 11"
    browser:       chrome
    browserVersion: latest
```

---

## [2.0.0] — 2026-05-04

### Added
- **Email Verification** — `mailbox().waitForEmail(criteria)` polls the inbox until a matching email arrives or the configured timeout expires; fluent criteria: `to(address)`, `.subject(text)`, `.containing(text)`, `.timeout(seconds)`; `Email` value object with `assertSubject()`, `assertBodyContains()`, `extractLink(linkText)` (finds href of anchor by visible text); `mailbox().clear()` purges the inbox; `email.autoClear: true` clears automatically before each test; `to(address)` shorthand available directly in `BaseTest` / `BaseJUnit5Test` (no static import needed)
- **Mailhog provider** — polls `GET /api/v2/messages`, parses multipart MIME; `DELETE /api/v1/messages` to clear; ideal for local dev and CI Docker
- **Mailtrap provider** — Mailtrap v1 REST API; `Api-Token` header auth; `PATCH /clean` to clear
- **Outlook / Office 365 provider** — Microsoft Graph API with app-only OAuth2 client credentials (no user sign-in); token auto-refreshed and cached; reads `GET /users/{mailbox}/messages`; deletes per-message; setup: register Azure AD app, grant `Mail.Read` + `Mail.ReadWrite` application permissions, admin consent
- **IMAP provider** — connects to any IMAP server (Gmail app passwords, Yahoo, corporate); SSL/STARTTLS configurable; requires optional `com.sun.mail:jakarta.mail:2.0.1` consumer dependency; helpful error thrown if jar missing

### Changed
- `BaseTest` / `BaseJUnit5Test` — new `mailbox()` and `to(address)` protected methods
- `SeleniumBootConfig` — new `email` block with `provider`, `timeoutSeconds`, `pollIntervalMs`, `autoClear`, `mailhog`, `mailtrap`, `outlook`, `imap` sub-sections

---

## [1.13.0] — 2026-05-03

### Added
- **`@NoBrowser`** — class- or method-level annotation that tells the framework to skip all browser operations for that test: no `WebDriver` created, no recording started, no screenshot captured, no trace saved, no driver quit; all other services (HTML report, step timeline, `ExecutionMetrics`, retry, CI gates, hooks, `@TestData`, `ScenarioContext`) continue to work; available in `BaseTest` (TestNG) and `BaseJUnit5Test` (JUnit 5); designed for database assertions, file checks, or any non-UI logic in tests that extend `BaseTest` rather than `BaseApiTest`

---

## [1.12.0] — 2026-05-03

### Added
- **Multi-Session Testing** — `withSession("alice", () -> { ... })` switches the active driver to a named session for the duration of the lambda and restores the previous driver on exit; `session("name")` returns the named `WebDriver` directly; all named sessions are automatically closed at test end; nested `withSession()` calls supported via a stack-based override in `DriverManager`; available in `BaseTest` (TestNG), `BaseJUnit5Test` (JUnit 5), and through `MultiSessionManager` directly
- **Database Assertions** — JDBC-backed `DbClient` with `assertRowExists(table, conditions)`, `assertNoRow(table, conditions)`, `assertRowCount(table, expected)`, `assertRowCount(table, where, expected)`, `query(sql, params).assertValue(column, expected)`, `query(sql, params).value(column)`, and `scalar(sql, params)`; plain `java.sql.DriverManager` — no ORM or extra dependency; named datasources via `db("reporting")`; connections cached per thread and closed automatically at test end; `DbAssertException extends AssertionError` so failures appear as test failures
- `sessions.maxPerTest` and `database` config blocks added to `SeleniumBootConfig`

---

## [1.11.0] — 2026-05-03

### Added
- **`@Retryable` for JUnit 5** — `SeleniumBootExtension` implements `InvocationInterceptor`; `interceptTestMethod` catches failures and retries the test method with full driver recreation between attempts; `@Retryable` can be placed on the method or the class; `maxAttempts` attribute overrides the global `retry.maxAttempts` config; `WebDriver` parameter arguments are re-resolved to the new driver on each retry attempt
- **`@Retryable` for Cucumber** — `RetryAnnotationTransformer` applies TestNG retry to `AbstractTestNGCucumberTests.runScenario`; the entire scenario reruns from step 1 with a fresh driver per retry; `CucumberHooks.beforeScenario` detects retries (testId already in metrics) and records retry count so the HTML report shows the retry badge

### Changed
- `@Retryable` — added `maxAttempts` attribute (default `-1` = use config); added `TYPE` target so it can be placed on a class to retry all its test methods; fully backward-compatible — existing usages without the attribute continue to work

---

## [1.10.0] — 2026-05-02

### Added
- **JUnit 5 Support** — `SeleniumBootExtension` (`@ExtendWith`) provides full lifecycle: driver creation in `beforeEach`, screenshot + error recording + AI analysis + trace + recording in `afterEach` (via `context.getExecutionException()`), per-suite driver cleanup in `afterAll`; `WebDriver` injectable as a test method parameter via `ParameterResolver`; `BaseJUnit5Test` base class with `getDriver()`, `getWait()`, `open()`, `$()`, `assertThat()`, `step()`; `@EnableSeleniumBoot` composed annotation; `SeleniumBootLauncherListener` (`TestExecutionListener`) generates HTML report, JSON metrics, and flakiness analysis when the JUnit Platform test plan finishes — registered automatically via `META-INF/services`; `junit-platform-launcher` declared as optional dependency; parallel execution supported via `junit-platform.properties`

---

## [1.9.0] — 2026-05-02

### Added
- **BDD / Cucumber Integration** — `BaseCucumberTest` (runner base), `BaseCucumberSteps` (step definition base with `getDriver()`, `open()`, `$()`, `assertThat()`), `CucumberHooks` (automatic driver lifecycle, metrics, screenshots, and report per scenario), `CucumberStepLogger` (Cucumber plugin that pipes Gherkin step names into the HTML report step timeline), `CucumberContext` (ThreadLocal `Scenario` holder); fully parallel-safe via ThreadLocal isolation; Scenario Outlines produce individual HTML report entries per example row; `cucumber.properties` support for IDE single-scenario execution; `cucumber-java` and `cucumber-testng` declared as optional dependencies — only pulled in by consumers who opt in

### Changed
- `TestExecutionListener` — skips all `onTest*` callbacks for Cucumber runner tests (`AbstractTestNGCucumberTests#runScenario`) to prevent duplicate HTML report entries when running BDD and TestNG tests in the same suite
- `ExecutionHook.onTestEnd` javadoc corrected: fires *before* driver quit, not after

---

## [1.8.0] — 2026-04-16

### Added
- **Self-Healing Locators** — `locators.selfHealing: true`; when `waitForVisible`/`waitForClickable` times out the framework automatically tries fallback strategies derived from the original `By` descriptor: extract `id` from CSS `#foo` / XPath `@id`, `name` from CSS `[name]` / XPath `@name`, text from XPath `text()`, class from CSS `.className`, `data-testid` and `placeholder` attributes; healed tests get a `⚠ healed` badge in the HTML report; `target/healed-locators.json` lists every healed locator for developer review
- **AI-Assisted Failure Analysis** — `ai.failureAnalysis: true` + `ai.apiKey: ${CLAUDE_API_KEY}`; on test failure calls `claude-haiku-4-5-20251001` (configurable via `ai.model`) with error, stack trace, step log, URL, and page title; the plain-English root-cause analysis + suggested fix is embedded in the HTML report failure detail panel; call bounded by `ai.timeoutSeconds` (default 20s); fully non-blocking — never affects suite outcome
- **Flakiness Prediction** — reads last `N` JSON run files from `target/metrics-history/`; classifies each test as `STABLE` (&lt;10% failure rate), `WATCH` (10–threshold%), or `HIGH` (≥threshold); results shown in a new **Flakiness Radar** card on the HTML report Dashboard; exported to `target/flakiness-report.json`; optional `flakiness.failOnHighFlakiness: true` CI gate

---

## [1.7.0] — 2026-04-16

### Added
- **Trace Viewer** — `tracing.enabled: true` in `selenium-boot.yml` generates a self-contained HTML trace file per failed test at `target/traces/{ClassName}/{testMethod}-trace.html`; the file embeds a clickable step timeline (each step shows its screenshot on click), a final-state screenshot taken at the moment of failure, the error message, and full stack trace; zero CDN dependencies — all CSS/JS are inlined; `captureOnPass: true` option to generate traces for passing tests too; HTML report shows a **"View Trace"** link in the failure detail panel

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
