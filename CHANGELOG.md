# Changelog

All notable changes to **Selenium Boot** are documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

---

### v3.1.1 — 2026-06-26

- **Per-engine report output directory** — the metrics JSON, HTML report, and metrics history now honor the `seleniumboot.reports.dir` system property (the same switch already used for the JUnit XML report), defaulting to `target` as before. This fixes the HTML report being **overwritten** when two test engines run in one build — e.g. a TestNG suite (Surefire) and JUnit 5 tests (Failsafe): point each engine's run at its own directory (`-Dseleniumboot.reports.dir=target/junit5`) and each produces a self-contained report instead of the last one clobbering the first.
- New `com.seleniumboot.reporting.ReportPaths` helper centralizes report path resolution.

### v3.1.0 — 2026-06-25

- **Accessibility-first locators** — new Playwright-style semantic locators on `BaseTest` and `BasePage`: `getByRole`, `getByText`, `getByLabel`, `getByPlaceholder`, `getByTestId`, `getByAltText`, `getByTitle`. They target the accessibility tree the user perceives instead of brittle CSS/DOM structure, so tests survive redesigns
- **`getByRole(Role)`** — 38 WAI-ARIA roles, each matching both implicit HTML elements (`<button>`, `<a href>`, `<h1>`…) and explicit `role="…"` attributes; refine with `.withName("Submit")` (accessible-name match) and `.withLevel(1)` (heading level)
- **Case-insensitive substring by default**, exact opt-in via `.exact()` — mirrors Playwright ergonomics; all locators flow through the existing auto-wait `Locator` chain (no `Thread.sleep`, no explicit waits)
- **`toBy()` escape hatch** — every semantic locator can return its synthesized Selenium `By` for drop-down to raw Selenium or `SmartLocator`
- **Configurable test-id attribute** — `locators.testIdAttribute` in `selenium-boot.yml` (default `data-testid`)
- Accessible-name computation follows ARIA precedence (`aria-label` → `aria-labelledby` → associated `<label>` → text → `value`/`alt`/`title`)

### v3.0.0 — 2026-06-21

- **TestRail + Xray Integration** — annotate any test method with `@TestRailCase("C1234")` or `@XrayTest("PROJ-123")` (both support arrays for multiple IDs); results are pushed automatically after each test completes — no extra code in `@Test` methods; TestRail results are pushed per-test immediately; Xray results are batch-imported at suite end for efficiency
- **TestRail** — creates a named run automatically (`testmanagement.testrail.autoCreateRun: true`); configurable `projectId`, `suiteId`, `runName`; maps PASSED→1, FAILED→5, SKIPPED→Retest(4); failure message is sent as the TestRail result comment
- **Xray Cloud** — authenticates via OAuth2 client credentials (`clientId` + `clientSecret`); uses `https://xray.cloud.getxpecto.com/api/v2` endpoints; supports `testPlanKey` linking
- **Xray Server/DC** — HTTP Basic auth (`jiraUrl` + `username` + `password`); posts to `/rest/raven/1.0/import/execution`; both modes use the same `@XrayTest` annotation
- **Zero extra dependencies** — clients use `java.net.http.HttpClient` (Java 17 built-in); no OkHttp, Apache HttpClient, or Jackson required for this feature
- **Works with TestNG and JUnit 5** — `SuiteExecutionListener` + `TestExecutionListener` handle TestNG; `SeleniumBootExtension` + `SeleniumBootLauncherListener` handle JUnit 5

### v2.6.0 — 2026-06-20

- **Gradle Build Support** — `testImplementation 'io.github.seleniumboot:selenium-boot:2.6.0'` in `build.gradle`; `test { useTestNG() }` is all that's needed; full docs with Groovy + Kotlin DSL samples, parallel config, JUnit 5 setup, and optional-dep table; `JUnitXmlReporter` auto-detects Maven vs Gradle by directory layout and writes to `build/test-results/test/` (Gradle) or `target/surefire-reports/` (Maven); override with `-Dseleniumboot.reports.dir=`; `FrameworkVersion` now reads `Implementation-Version` from MANIFEST.MF (set by `maven-jar-plugin` and Gradle `jar` task) so `FrameworkVersion.get()` returns the correct version in both build tools

### v2.5.0 — 2026-06-20

- **Accessibility Assertions (axe-core)** — `accessibility().withTags("wcag2a","wcag21aa").withLevel(Impact.SERIOUS).excluding("#cookie-banner").run()` runs an axe-core WCAG scan on the active page; axe-core 4.10.2 bundled in the JAR — no CDN, no extra Maven dependency; `withContext("#main-form")` scopes the scan to a subtree; `collect()` returns raw `AccessibilityResult` for custom inspection; detailed `AssertionError` message shows rule ID, severity, fix guidance, element selector, and docs URL for every failing node; available in `BaseTest` and `BaseJUnit5Test`

### v2.4.0 — 2026-05-19

- **Performance Assertions** — `assertPerformance().lcp().isBelow(2500).fcp().isBelow(1800).ttfb().isBelow(600).cls().isBelow(0.1)` — Core Web Vitals via browser-native `window.performance` API, no extra tool needed; LCP/CLS Chrome/Edge only, others all browsers; unavailable metrics silently skipped; `performance.captureOnEveryTest: true` shows ⚡ metrics strip in HTML report

### v2.3.0 — 2026-05-17

- **Test Quarantine** — `selenium-quarantine.yml` committed to the repo lists tests to skip; survives fresh CI clones; plain string or structured-with-reason format; class-only entry skips all methods in the class; Cucumber support via `@quarantine` tag; `quarantine.enabled: false` disables without editing the file

### v2.2.0 — 2026-05-12

- **External `@TestData` sources** — `@TestData("csv:testdata/logins.csv")` (built-in parser), `@TestData(value="excel:testdata/users.xlsx", sheet="Login")` (Apache POI optional dep), `@TestData("db:SELECT username, password FROM test_users LIMIT 1")` (JDBC via `database` config); `row` attribute selects zero-based data row (header excluded); type coercion: integers, doubles, booleans
- **`TestClock`** — `clock().set("2030-01-01T00:00:00Z")` injects a JS `Date` override into the browser; `clock().advance(Duration.ofDays(30))` fast-forwards from current mock; `clock().reset()` restores real time; auto-reset after every test (no cleanup needed); available via `clock()` in `BaseTest` and `BaseJUnit5Test`

### v2.1.0 — 2026-05-04

- **BrowserStack integration** — `execution.mode: browserstack` + `browserstack.username/accessKey/os/osVersion/browser/browserVersion`; W3C `bstack:options` capabilities; mobile device support via `device` + `realMobile`; session dashboard URL auto-injected into HTML report as "☁ View Session" link
- **Sauce Labs integration** — `execution.mode: saucelabs` + `saucelabs.username/accessKey/region/platformName/browser/browserVersion`; three regions: `us-west-1`, `eu-central`, `apac-southeast`; W3C `sauce:options` capabilities; session dashboard URL in HTML report
- Existing `mode: remote` (self-hosted Selenium Grid) unchanged — all three remote modes coexist

### v2.0.0 — 2026-05-04

- **Email Verification** — `mailbox().waitForEmail(to("user@example.com"))` polls until an email arrives; `email.assertSubject()`, `email.assertBodyContains()`, `email.extractLink(linkText)` for anchor extraction; `mailbox().clear()` purges the inbox; `email.autoClear: true` clears automatically before each test
- **Four backends**: Mailhog (local/Docker), Mailtrap (hosted sandbox), Outlook/Office 365 (Microsoft Graph API — app-only OAuth2, no user login), IMAP (Gmail, Yahoo, any standards-compliant server via optional `jakarta.mail` dep)
- Outlook config: `tenantId`, `clientId`, `clientSecret`, `mailbox` — OAuth2 token auto-refreshed and cached

### v1.13.0 — 2026-05-03

- **`@NoBrowser`** — annotate a test class or method to skip WebDriver creation entirely; no Chrome/Firefox window opened, no screenshot/recording/trace; all other framework services (report, steps, metrics, retry, hooks) still active; ideal for database assertions, API-only tests that extend `BaseTest`, and any test that does not touch the browser

### v1.12.0 — 2026-05-03

- **Multi-Session Testing** — `withSession("alice", () -> { ... })` runs a lambda with a named browser session active; `session("name")` returns the raw `WebDriver`; all named sessions are automatically closed at test end; nested `withSession()` calls supported via stack-based driver restoration; available in `BaseTest` and `BaseJUnit5Test`
- **Database Assertions** — `db().assertRowExists(table, conditions)`, `db().assertNoRow()`, `db().assertRowCount()`, `db().query(sql, params).assertValue(column, expected)`, `db().scalar(sql, params)`; plain JDBC, no ORM dependency; named datasources via `db("reporting")`; connections pooled per thread and closed automatically at test end
- New `sessions.maxPerTest` and `database.*` config blocks in `selenium-boot.yml`

### v1.11.0 — 2026-05-03

- **`@Retryable` for JUnit 5** — `InvocationInterceptor` in `SeleniumBootExtension` retries failed test methods with full driver recreation between attempts; `@Retryable(maxAttempts = N)` on method or class overrides global config
- **`@Retryable` for Cucumber** — entire scenario reruns from step 1 with a fresh driver via `RetryAnnotationTransformer`; retry detected in `CucumberHooks` and shown as retry badge in HTML report
- `@Retryable` — new `maxAttempts` attribute + class-level target; fully backward-compatible

### v1.10.0 — 2026-05-02

- **JUnit 5 Support** — `SeleniumBootExtension` (`@ExtendWith`) handles driver lifecycle, screenshot on failure, AI analysis, trace, and recording; `WebDriver` injectable as test method parameter; `BaseJUnit5Test` base class with `getDriver()`, `getWait()`, `open()`, `$()`, `assertThat()`, `step()`; `@EnableSeleniumBoot` composed annotation; `SeleniumBootLauncherListener` generates HTML report when the JUnit Platform test plan finishes (registered via ServiceLoader — no config needed); parallel execution via `junit-platform.properties`

### v1.9.0 — 2026-05-02

- **BDD / Cucumber Integration** — `BaseCucumberTest` runner base + `BaseCucumberSteps` step definition base (`getDriver()`, `open()`, `$()`, `assertThat()`); `CucumberHooks` manages driver lifecycle, metrics, and screenshots per scenario automatically; `CucumberStepLogger` plugin streams Gherkin step names into the HTML report step timeline; `cucumber-java` and `cucumber-testng` declared as optional — consumers add their own version; `cucumber.properties` support for IDE single-scenario runs; Scenario Outlines produce individual report entries per example row

### v1.8.0 — 2026-04-16

- **Self-Healing Locators** — `locators.selfHealing: true`; when `waitForVisible`/`waitForClickable` times out, framework automatically tries fallback strategies: extract `id` from CSS `#foo` or XPath `@id`, `name` from CSS `[name]` or XPath `@name`, text from XPath `text()`, class from CSS `.btn`, `data-testid`; healed tests get `⚠ healed` badge in HTML report; `target/healed-locators.json` lists every healed locator after suite
- **AI-Assisted Failure Analysis** — `ai.failureAnalysis: true` + `ai.apiKey: ${CLAUDE_API_KEY}`; on test failure calls `claude-haiku-4-5-20251001` with error message, stack trace, step log, URL, and page title; plain-English root-cause + fix suggestion embedded in HTML report below the stack trace; bounded by `ai.timeoutSeconds` (default 20s); fully non-blocking — never affects suite result
- **Flakiness Prediction** — reads last `flakiness.historyRuns` (default 20) JSON files from `target/metrics-history/`; computes per-test failure rate; classifies as `STABLE` (<10%), `WATCH` (10–33%), `HIGH` (≥33%); **Flakiness Radar** card in HTML report; `target/flakiness-report.json`; `flakiness.failOnHighFlakiness: true` to gate builds

### v1.7.0 — 2026-04-16

- **Trace Viewer** — `tracing.enabled: true` generates a self-contained dark-themed `target/traces/{Class}/{method}-trace.html` per failed test; embeds step timeline with screenshots, final-state screenshot, error message + stack trace; zero CDN dependencies; `captureOnPass: true` option to trace passing tests too; "View Trace" link appears in the HTML report's failure detail panel

### v1.6.0 — 2026-04-16

- **Visual regression testing** — `VisualAssert.assertScreenshot()` pixel-by-pixel comparison; auto-creates baseline on first run; diff image saved to `target/visual-diffs/`; configurable tolerance via `VisualTolerance.of(n)`; `-DupdateBaselines=true` to regenerate
- **Mobile device emulation** — `DeviceEmulator.emulate("iPhone 14")` / `emulateDevice()` in `BasePage`/`BaseTest`; CDP-based on Chrome/Edge (viewport, scale factor, UA); window-resize fallback on Firefox; 6 built-in profiles (iPhone 14, iPhone SE, Pixel 7, Galaxy S23, iPad, iPad Pro 12) via `DeviceProfiles` registry
- **Clipboard helpers** — `ClipboardHelper.write/read/clear()` backed by a reliable JS global store
- **GeoLocation mock** — CDP-based on Chrome/Edge, JS `navigator.geolocation` override on Firefox
- **Network interception** — `NetworkMock.stub(pattern)` stubs API responses via CDP `Fetch` domain; glob patterns, custom status, delay, auto-cleanup after each test
- **Browser storage helpers** — `StorageHelper.localStorage()`, `sessionStorage()`, `cookies()` for reading/writing browser storage in tests
- **Fluent Locator API** — `$(css)` / `$(By)` chainable locator: `filter()`, `withText()`, `within()`, `nth()`, auto-wait terminals
- **Web-First Assertions** — `assertThat(By/Locator)` with auto-retry: `isVisible`, `isHidden`, `hasText`, `containsText`, `hasAttribute`, `hasClass`, `count`

### v1.3.0 — 2026-04-07

- **Shadow DOM helpers** — `ShadowDom` utility + 7 `BasePage` methods (`shadowFind`, `shadowFindAll`, `shadowClick`, `shadowType`, `shadowGetText`, `shadowPierce`, `shadowExists`)
- **Alert handling fix** — `unhandledPromptBehavior: ignore` on all driver providers; `BasePage.getAndAcceptAlert()` convenience method
- **Component-aware waits** — `WaitEngine.waitForAngular()` (Angular 2+/AngularJS 1.x) and `WaitEngine.waitForReactHydration()` (React 18/17/16, Next.js)
- **Enhanced HTML report** — pass rate gauge, donut chart, retry badges, expandable error/stack trace rows, filter bar, search, dark mode, slowest-5 section
- **JUnit XML error details** — `<failure message>` now contains the actual assertion message and full stack trace
- **Allure adapter** — opt-in Allure 2 JSON result files in `target/allure-results/` (`reporting.allureEnabled: true`)
- **Slack / Teams notifications** — webhook-based post-suite summary with pass/fail counts and failed test list
- **`@DependsOnApi`** — skip test immediately (before browser open) if a dependent HTTP endpoint is unreachable; repeatable; cached per suite

### v1.1.1 — 2026-03-28

- **Schema validation** — `ApiResponse.assertSchema("schemas/user.json")` validates response body against a JSON Schema file; requires `com.networknt:json-schema-validator:1.4.3` on classpath
- **`@UseAuth` annotation** — apply named auth strategy from config to any test method or class
- **`ApiAuth.oauth2()`** — OAuth2 client credentials flow with automatic token caching and expiry refresh
- **`ApiClient.setGlobalAuth()` / `clearGlobalAuth()`** — set auth once per suite, applied automatically to all requests

### v1.1.0 — 2026-03-25

- **`BaseApiTest`** — pure API test base class; no browser started; full framework lifecycle (reporting, `@TestData`, retry, CI gates)
- **`ApiClient`** — fluent HTTP client backed by Java's built-in `HttpClient`; `GET`, `POST`, `PUT`, `PATCH`, `DELETE`; auto step-logging
- **`ApiResponse`** — rich response wrapper; JSONPath extraction (`$.user.id`), `asObject(Class)`, fluent assertions (`assertStatus`, `assertJson`, `assertBodyContains`)
- **`ApiAuth`** — `bearerToken(token)`, `basicAuth(user, pass)` auth strategies
- **`ScenarioContext`** — thread-local in-test store; `ctx().set/get`; auto-cleared after each test
- **`SuiteContext`** — global thread-safe store for cross-test state; `suiteCtx().set/get`
- **`apiClient()`, `ctx()`, `suiteCtx()`** added to `BaseTest` for hybrid UI+API tests

### v0.10.0 — 2026-03-22

- **`@TestData`** — annotation-driven test data injection; loads JSON/YAML from `src/test/resources/testdata/`; env-specific override (`admin.staging.json` overrides `admin.json` when `-Denv=staging`); `getTestData()` in `BaseTest`
- **Browser matrix** — `browser.matrix: [chrome, firefox]` in YAML runs every test on every browser in one `mvn test`; `Browser` column in HTML report; per-browser JUnit XML for Jenkins matrix view
- **`SessionCache`** — `SessionCache.store("name")` / `SessionCache.restore("name")`; global cross-thread session reuse; reduces repeated login overhead in large parallel suites
- **SoftAssert** — `softAssert().that(condition, "message")` collects failures without throwing; framework flushes at test end; single screenshot captured; all failures appear as individual step entries in the report

### v0.9.6 — 2026-03-21

- **DownloadManager browser auto-configuration** — Chrome and Firefox automatically configure download directory from `browser.downloadDir` config; no save dialog shown; partial downloads (.crdownload, .part) filtered out
- **Remote mode guard** — download prefs skipped when `execution.mode: remote` (Grid sessions download to node filesystem, not local)
- **File upload helper** — `BasePage.upload(By, String)` resolves paths via absolute → classpath → project-root; CI-safe

### v0.9.5 — 2026-03-21

- **ConsoleErrorCollector auto-integration** — when `browser.captureConsoleErrors: true`, the JS shim is auto-injected on every `open()` call and errors are auto-collected at test end as `WARN` step entries in the report
- **`failOnConsoleErrors` enforcement** — when `browser.failOnConsoleErrors: true`, a passing test with JS errors is marked FAILED with a clear error listing the detected errors
- **`StepStatus.WARN`** — new step status for JS error entries (does not affect test outcome)

### v0.9.4 — 2026-03-20

- **iFrame helpers expanded** — `withinFrameName(String, Runnable)` added; `withinFrame`, `withinFrameIndex`, `withinFrameName` now support nested frames (inner calls restore to `parentFrame()`, outermost restores to `defaultContent()`)

### v0.9.3 — 2026-03-20

- **Bug fix** — alert methods (`acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert`) in `BasePage` now use `this.driver` directly instead of `WaitEngine`/`DriverManager`, matching the driver reference used by the page object
- **Bug fix** — `@PreCondition` failure error message now shows the real exception instead of "null" (unwraps `InvocationTargetException`)

### v0.9.2 — 2026-03-20

- **Bug fix** — precondition failure now throws `SkipException` (not `RuntimeException`); prevents retry from firing and a second browser from opening on `@PreCondition` setup failures
- **Bug fix** — `maxAttempts: 0` in YAML now respected; changed `int` field default to nullable `Integer` so intentional `0` is never silently overridden by programmatic defaults

### v0.9.1 — 2026-03-19

- **Alert wait fix** — `acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert` now use `WaitEngine.waitForAlert()` — no more `NoAlertPresentException` on slow pages
- **`WaitEngine.waitForAlert()`** — new explicit wait for browser alert presence
- **`BasePage.smartFind()`** — convenience wrapper; use inside page objects without passing the driver

### v0.9.0 — 2026-03-18

- **`BasePage` expanded** — dropdowns (`selectByText`, `selectByValue`, `selectByIndex`, `getSelectedOption`), alerts (`acceptAlert`, `dismissAlert`, `getAlertText`, `typeInAlert`), mouse actions (`hover`, `doubleClick`, `rightClick`), scroll (`scrollTo`, `scrollToTop`, `scrollToBottom`), JS fallbacks (`jsClick`, `jsType`)

### v0.8.0 — 2026-03-17

- **`BasePage`** — page object base class: `click`, `type`, `getText`, `isDisplayed`, `withinFrame`, `withinFrameIndex`, `upload`
- **`SmartLocator`** — tries multiple `By` strategies in order, returns first visible element
- **`DownloadManager`** — `waitForFile`, `waitForAnyFile`, `clearDownloads` with partial-download detection
- **`ConsoleErrorCollector`** — JS console error capture (Chrome via WebDriver logs, Firefox via injected shim)
- **`@PreCondition`** — session-aware pre-conditions with cookie + localStorage caching per thread
- **`@ConditionProvider`** + `BaseConditions` — define named condition providers, registered via SPI
- **`@SeleniumBootApi`** — annotation marking stable public API with `since` version
- **`FrameworkVersion`** + `minFrameworkVersion()` — runtime version checks, plugin compatibility enforcement
- **Config additions** — `browser.downloadDir`, `browser.captureConsoleErrors`, `browser.failOnConsoleErrors`

### v0.7.0 — 2026-03-16

- **`StepLogger`** — named test steps with timestamps and optional per-step screenshots
- **Step timeline** — step-by-step execution timeline in the HTML report Failures tab
- **Tabbed HTML report** — Dashboard, Test Cases, and Failures tabs with collapsible rows
- **Browser lifecycle** — `browser.lifecycle: per-test | per-suite` configuration

### v0.6.0 — 2025-12-01

- **Advanced HTML report** — pass rate gauge, donut chart, slowest tests card, retry badges
- **Self-contained report** — screenshots base64-encoded, single file, no external dependencies

### v0.5.0 — 2025-10-15

- **Retry support** — `retry.enabled` + `retry.maxAttempts` in `selenium-boot.yml`
- **`@Retryable`** — per-method retry override
- **Retry metrics** — retry counts tracked in `ExecutionMetrics` and exported to JSON

### v0.4.0 — 2025-08-20

- **CI auto-detection** — GitHub Actions, Jenkins, CircleCI, GitLab CI; forces headless, tunes thread count
- **Container detection** — Docker and Kubernetes; auto-applies Chrome container flags
- **JUnit XML reporter** — `target/surefire-reports/TEST-SeleniumBoot.xml`
- **Build quality gates** — `BuildThresholdEnforcer` enforces pass-rate and flaky-test thresholds
- **CI templates** — GitHub Actions workflow and Jenkinsfile included

### v0.3.0 — 2025-06-10

- **Plugin system** — `SeleniumBootPlugin` + `PluginRegistry` with SPI discovery
- **Custom driver providers** — `NamedDriverProvider` + `DriverProviderRegistry`
- **Custom report adapters** — `ReportAdapter` + `ReportAdapterRegistry`
- **Lifecycle hooks** — `ExecutionHook` + `HookRegistry`
- **`SeleniumBootDefaults`** — programmatic config defaults for shared test-base JARs

### v0.2.0 — 2025-04-05

- **`WaitEngine`** — fluent explicit wait API
- **Thread-safe config** — `SeleniumBootContext` with `AtomicReference`
- **Session semaphore** — `maxActiveSessions` cap with fair wait
- **Global retry** — `retry.enabled: true` retries all tests without `@Retryable`

### v0.1.0 — 2025-02-01

- Initial release — Chrome/Firefox, TestNG integration, `selenium-boot.yml`, basic HTML report, screenshot on failure
