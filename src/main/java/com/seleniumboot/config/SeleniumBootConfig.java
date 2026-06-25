package com.seleniumboot.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SeleniumBootConfig {

    private Browser browser;
    private Execution execution;
    private Retry retry;
    private Timeouts timeouts;
    private Ci ci;
    private Api api;

    // --- getters ---
    public Browser getBrowser() {
        return browser;
    }

    public Execution getExecution() {
        return execution;
    }
    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    // --- setters (required for SnakeYAML) ---
    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public Timeouts getTimeouts() {
        return timeouts;
    }
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
    }

    public Ci getCi() {
        return ci;
    }
    public void setCi(Ci ci) {
        this.ci = ci;
    }

    private Network network;
    public Network getNetwork() { return network; }
    public void setNetwork(Network network) { this.network = network; }

    private TestManagement testManagement;
    public TestManagement getTestManagement() { return testManagement; }
    public void setTestManagement(TestManagement testManagement) { this.testManagement = testManagement; }

    public static final class Network {
        private boolean interceptEnabled = false;
        public boolean isInterceptEnabled() { return interceptEnabled; }
        public void setInterceptEnabled(boolean interceptEnabled) { this.interceptEnabled = interceptEnabled; }
    }

    // =========================

    public static final class Browser {
        private String name;
        private boolean headless;
        private List<String> arguments;
        private Map<String, Object> capabilities;

        /**
         * Controls when the WebDriver session is closed.
         * <ul>
         *   <li>{@code per-test}  (default) — browser closes after every test method</li>
         *   <li>{@code per-suite} — browser stays open for the entire suite; one instance
         *       per thread, closed only when the suite finishes. Saves startup time on
         *       large sequential suites.</li>
         * </ul>
         */
        private String lifecycle = "per-test";
        private String downloadDir = "./target/downloads";
        private boolean captureConsoleErrors = false;
        private boolean failOnConsoleErrors = false;
        private List<String> matrix = Collections.emptyList();
        private String device;   // optional device profile name, e.g. "iPhone 14"

        public String getDevice()              { return device; }
        public void   setDevice(String device) { this.device = device; }

        public String getDownloadDir() { return downloadDir; }
        public void setDownloadDir(String downloadDir) { this.downloadDir = downloadDir; }

        public boolean isCaptureConsoleErrors() { return captureConsoleErrors; }
        public void setCaptureConsoleErrors(boolean captureConsoleErrors) { this.captureConsoleErrors = captureConsoleErrors; }

        public boolean isFailOnConsoleErrors() { return failOnConsoleErrors; }
        public void setFailOnConsoleErrors(boolean failOnConsoleErrors) { this.failOnConsoleErrors = failOnConsoleErrors; }

        public List<String> getMatrix() { return matrix; }
        public void setMatrix(List<String> matrix) { this.matrix = matrix != null ? matrix : Collections.emptyList(); }

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        public Map<String, Object> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Map<String, Object> capabilities) {
            this.capabilities = capabilities;
        }

        public String getName() {
            return name;
        }

        public boolean isHeadless() {
            return headless;
        }

        // setters REQUIRED
        public void setName(String name) {
            this.name = name;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
        }

        public String getLifecycle() { return lifecycle; }
        public void setLifecycle(String lifecycle) { this.lifecycle = lifecycle; }
    }

    public static final class Execution {
        private String mode;
        private String baseUrl;
        private String gridUrl;
        private String parallel = "none";
        private int threadCount = 1;
        private int maxActiveSessions = 5;

        public String getMode() {
            return mode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        // setters REQUIRED
        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public void setGridUrl(String gridUrl) {
            this.gridUrl = gridUrl;
        }

        public String getGridUrl() {
            return gridUrl;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public String getParallel() {
            return parallel;
        }

        public void setParallel(String parallel) {
            this.parallel = parallel;
        }

        public int getMaxActiveSessions() {
            return maxActiveSessions;
        }

        public void setMaxActiveSessions(int maxActiveSessions) {
            this.maxActiveSessions = maxActiveSessions;
        }

        private BrowserStack browserstack = new BrowserStack();
        private SauceLabs    saucelabs    = new SauceLabs();

        public BrowserStack getBrowserstack()                  { return browserstack; }
        public void         setBrowserstack(BrowserStack v)    { this.browserstack = v; }
        public SauceLabs    getSaucelabs()                     { return saucelabs; }
        public void         setSaucelabs(SauceLabs v)          { this.saucelabs = v; }

        public static final class BrowserStack {
            private String  username;
            private String  accessKey;
            private String  os;
            private String  osVersion;
            private String  browser        = "chrome";
            private String  browserVersion = "latest";
            private String  device;
            private boolean realMobile     = true;
            private java.util.Map<String, Object> capabilities = new java.util.LinkedHashMap<>();

            public String  getUsername()              { return username; }
            public void    setUsername(String v)      { this.username = v; }
            public String  getAccessKey()             { return accessKey; }
            public void    setAccessKey(String v)     { this.accessKey = v; }
            public String  getOs()                    { return os; }
            public void    setOs(String v)            { this.os = v; }
            public String  getOsVersion()             { return osVersion; }
            public void    setOsVersion(String v)     { this.osVersion = v; }
            public String  getBrowser()               { return browser; }
            public void    setBrowser(String v)       { this.browser = v; }
            public String  getBrowserVersion()        { return browserVersion; }
            public void    setBrowserVersion(String v){ this.browserVersion = v; }
            public String  getDevice()                { return device; }
            public void    setDevice(String v)        { this.device = v; }
            public boolean isRealMobile()             { return realMobile; }
            public void    setRealMobile(boolean v)   { this.realMobile = v; }
            public java.util.Map<String, Object> getCapabilities() { return capabilities; }
            public void setCapabilities(java.util.Map<String, Object> v) { this.capabilities = v != null ? v : new java.util.LinkedHashMap<>(); }
        }

        public static final class SauceLabs {
            private String  username;
            private String  accessKey;
            private String  region         = "us-west-1";
            private String  platformName   = "Windows 11";
            private String  browser        = "chrome";
            private String  browserVersion = "latest";
            private java.util.Map<String, Object> capabilities = new java.util.LinkedHashMap<>();

            public String  getUsername()              { return username; }
            public void    setUsername(String v)      { this.username = v; }
            public String  getAccessKey()             { return accessKey; }
            public void    setAccessKey(String v)     { this.accessKey = v; }
            public String  getRegion()                { return region; }
            public void    setRegion(String v)        { this.region = v; }
            public String  getPlatformName()          { return platformName; }
            public void    setPlatformName(String v)  { this.platformName = v; }
            public String  getBrowser()               { return browser; }
            public void    setBrowser(String v)       { this.browser = v; }
            public String  getBrowserVersion()        { return browserVersion; }
            public void    setBrowserVersion(String v){ this.browserVersion = v; }
            public java.util.Map<String, Object> getCapabilities() { return capabilities; }
            public void setCapabilities(java.util.Map<String, Object> v) { this.capabilities = v != null ? v : new java.util.LinkedHashMap<>(); }
        }
    }

    public static final class Retry {
        private boolean enabled = true;
        private int maxAttempts = 1;
        private boolean maxAttemptsSet = false;  // true only when YAML explicitly sets the value

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        /** Returns null when maxAttempts was not explicitly set by YAML (used by defaults logic). */
        public Integer getRawMaxAttempts() {
            return maxAttemptsSet ? maxAttempts : null;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            this.maxAttemptsSet = true;
        }
    }

    public static final class Timeouts {
        private int explicit;
        private int pageLoad;

        public int getExplicit() {
            return explicit;
        }

        public void setExplicit(int explicit) {
            this.explicit = explicit;
        }

        public int getPageLoad() {
            return pageLoad;
        }

        public void setPageLoad(int pageLoad) {
            this.pageLoad = pageLoad;
        }
    }

    /**
     * CI/CD build quality gates.
     * All thresholds are disabled by default (0 / -1).
     */
    public static final class Ci {
        /**
         * Minimum pass rate (0–100) required to pass the build.
         * 0 means disabled. Example: 80 → build fails if fewer than 80% of tests pass.
         */
        private double failOnPassRateBelow = 0;

        /**
         * Maximum number of flaky tests (retried but eventually passed) allowed.
         * -1 means disabled.
         */
        private int maxFlakyTests = -1;

        public double getFailOnPassRateBelow() {
            return failOnPassRateBelow;
        }
        public void setFailOnPassRateBelow(double failOnPassRateBelow) {
            this.failOnPassRateBelow = failOnPassRateBelow;
        }

        public int getMaxFlakyTests() {
            return maxFlakyTests;
        }
        public void setMaxFlakyTests(int maxFlakyTests) {
            this.maxFlakyTests = maxFlakyTests;
        }
    }

    private Recording recording;
    public Recording getRecording() { return recording; }
    public void setRecording(Recording recording) { this.recording = recording; }

    private Reporting reporting;
    public Reporting getReporting() { return reporting; }
    public void setReporting(Reporting reporting) { this.reporting = reporting; }

    private Notifications notifications;
    public Notifications getNotifications() { return notifications; }
    public void setNotifications(Notifications notifications) { this.notifications = notifications; }

    public static final class Notifications {
        private Slack slack;
        private Teams teams;

        public Slack getSlack() { return slack; }
        public void  setSlack(Slack slack) { this.slack = slack; }
        public Teams getTeams() { return teams; }
        public void  setTeams(Teams teams) { this.teams = teams; }

        public static final class Slack {
            private String  webhookUrl;
            private boolean notifyOnFailureOnly = false;

            public String  getWebhookUrl()                      { return webhookUrl; }
            public void    setWebhookUrl(String webhookUrl)     { this.webhookUrl = webhookUrl; }
            public boolean isNotifyOnFailureOnly()              { return notifyOnFailureOnly; }
            public void    setNotifyOnFailureOnly(boolean v)    { this.notifyOnFailureOnly = v; }
        }

        public static final class Teams {
            private String  webhookUrl;
            private boolean notifyOnFailureOnly = false;

            public String  getWebhookUrl()                      { return webhookUrl; }
            public void    setWebhookUrl(String webhookUrl)     { this.webhookUrl = webhookUrl; }
            public boolean isNotifyOnFailureOnly()              { return notifyOnFailureOnly; }
            public void    setNotifyOnFailureOnly(boolean v)    { this.notifyOnFailureOnly = v; }
        }
    }

    public static final class Reporting {
        private boolean allureEnabled = false;

        public boolean isAllureEnabled()                      { return allureEnabled; }
        public void    setAllureEnabled(boolean allureEnabled) { this.allureEnabled = allureEnabled; }
    }

    public static final class Recording {
        private boolean enabled            = false;
        private int     fps                = 2;
        private int     maxDurationSeconds = 60;

        public boolean isEnabled()                    { return enabled; }
        public void    setEnabled(boolean enabled)    { this.enabled = enabled; }

        public int  getFps()                          { return fps; }
        public void setFps(int fps)                   { this.fps = fps; }

        public int  getMaxDurationSeconds()                          { return maxDurationSeconds; }
        public void setMaxDurationSeconds(int maxDurationSeconds)    { this.maxDurationSeconds = maxDurationSeconds; }
    }

    private Visual visual;
    public Visual getVisual() { return visual; }
    public void setVisual(Visual visual) { this.visual = visual; }

    private Tracing tracing;
    public Tracing getTracing() { return tracing; }
    public void setTracing(Tracing tracing) { this.tracing = tracing; }

    private Locators locators;
    public Locators getLocators() { return locators; }
    public void setLocators(Locators locators) { this.locators = locators; }

    public static final class Locators {
        private boolean selfHealing = false;
        private String  testIdAttribute = "data-testid";

        public boolean isSelfHealing()           { return selfHealing; }
        public void    setSelfHealing(boolean v) { this.selfHealing = v; }

        public String  getTestIdAttribute()             { return testIdAttribute; }
        public void    setTestIdAttribute(String value) { this.testIdAttribute = value; }
    }

    private Ai ai;
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }

    public static final class Ai {
        private boolean failureAnalysis = false;
        private String  apiKey          = null;
        private String  model           = "claude-haiku-4-5-20251001";
        private int     timeoutSeconds  = 20;

        public boolean isFailureAnalysis()               { return failureAnalysis; }
        public void    setFailureAnalysis(boolean v)     { this.failureAnalysis = v; }

        public String  getApiKey()                       { return apiKey; }
        public void    setApiKey(String v)               { this.apiKey = v; }

        public String  getModel()                        { return model; }
        public void    setModel(String v)                { this.model = v; }

        public int     getTimeoutSeconds()               { return timeoutSeconds; }
        public void    setTimeoutSeconds(int v)          { this.timeoutSeconds = v; }
    }

    private Flakiness flakiness;
    public Flakiness getFlakiness() { return flakiness; }
    public void setFlakiness(Flakiness flakiness) { this.flakiness = flakiness; }

    public static final class Flakiness {
        private int     historyRuns          = 20;
        private double  highRiskThreshold    = 33.0;
        private boolean failOnHighFlakiness  = false;

        public int     getHistoryRuns()                   { return historyRuns; }
        public void    setHistoryRuns(int v)              { this.historyRuns = v; }

        public double  getHighRiskThreshold()             { return highRiskThreshold; }
        public void    setHighRiskThreshold(double v)     { this.highRiskThreshold = v; }

        public boolean isFailOnHighFlakiness()            { return failOnHighFlakiness; }
        public void    setFailOnHighFlakiness(boolean v)  { this.failOnHighFlakiness = v; }
    }

    public static final class Tracing {
        private boolean enabled       = false;
        private boolean captureOnPass = false;

        public boolean isEnabled()              { return enabled; }
        public void    setEnabled(boolean v)    { this.enabled = v; }

        public boolean isCaptureOnPass()           { return captureOnPass; }
        public void    setCaptureOnPass(boolean v) { this.captureOnPass = v; }
    }

    public static final class Visual {
        private String  baselineDir      = "src/test/resources/baselines";
        private String  diffDir          = "target/visual-diffs";
        private double  defaultTolerance = 0;
        private boolean updateBaselines  = false;

        public String  getBaselineDir()                    { return baselineDir; }
        public void    setBaselineDir(String v)            { this.baselineDir = v; }

        public String  getDiffDir()                        { return diffDir; }
        public void    setDiffDir(String v)                { this.diffDir = v; }

        public double  getDefaultTolerance()               { return defaultTolerance; }
        public void    setDefaultTolerance(double v)       { this.defaultTolerance = v; }

        public boolean isUpdateBaselines()                 { return updateBaselines; }
        public void    setUpdateBaselines(boolean v)       { this.updateBaselines = v; }
    }

    private Email email;
    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public static final class Email {
        private String  provider        = "mailhog";
        private int     timeoutSeconds  = 30;
        private int     pollIntervalMs  = 1000;
        private boolean autoClear       = false;

        public String  getProvider()              { return provider; }
        public void    setProvider(String v)      { this.provider = v; }
        public int     getTimeoutSeconds()        { return timeoutSeconds; }
        public void    setTimeoutSeconds(int v)   { this.timeoutSeconds = v; }
        public int     getPollIntervalMs()        { return pollIntervalMs; }
        public void    setPollIntervalMs(int v)   { this.pollIntervalMs = v; }
        public boolean isAutoClear()              { return autoClear; }
        public void    setAutoClear(boolean v)    { this.autoClear = v; }

        private Mailhog  mailhog  = new Mailhog();
        private Mailtrap mailtrap = new Mailtrap();
        private Outlook  outlook  = new Outlook();
        private Imap     imap     = new Imap();

        public Mailhog  getMailhog()              { return mailhog; }
        public void     setMailhog(Mailhog v)     { this.mailhog = v; }
        public Mailtrap getMailtrap()             { return mailtrap; }
        public void     setMailtrap(Mailtrap v)   { this.mailtrap = v; }
        public Outlook  getOutlook()              { return outlook; }
        public void     setOutlook(Outlook v)     { this.outlook = v; }
        public Imap     getImap()                 { return imap; }
        public void     setImap(Imap v)           { this.imap = v; }

        public static final class Mailhog {
            private String host = "localhost";
            private int    port = 8025;
            public String getHost()          { return host; }
            public void   setHost(String v)  { this.host = v; }
            public int    getPort()          { return port; }
            public void   setPort(int v)     { this.port = v; }
        }

        public static final class Mailtrap {
            private String apiToken;
            private String accountId;
            private String inboxId;
            public String getApiToken()           { return apiToken; }
            public void   setApiToken(String v)   { this.apiToken = v; }
            public String getAccountId()          { return accountId; }
            public void   setAccountId(String v)  { this.accountId = v; }
            public String getInboxId()            { return inboxId; }
            public void   setInboxId(String v)    { this.inboxId = v; }
        }

        public static final class Outlook {
            private String tenantId;
            private String clientId;
            private String clientSecret;
            private String mailbox;
            public String getTenantId()              { return tenantId; }
            public void   setTenantId(String v)      { this.tenantId = v; }
            public String getClientId()              { return clientId; }
            public void   setClientId(String v)      { this.clientId = v; }
            public String getClientSecret()          { return clientSecret; }
            public void   setClientSecret(String v)  { this.clientSecret = v; }
            public String getMailbox()               { return mailbox; }
            public void   setMailbox(String v)       { this.mailbox = v; }
        }

        public static final class Imap {
            private String  host;
            private int     port     = 993;
            private boolean ssl      = true;
            private String  username;
            private String  password;
            private String  folder   = "INBOX";
            public String  getHost()              { return host; }
            public void    setHost(String v)      { this.host = v; }
            public int     getPort()              { return port; }
            public void    setPort(int v)         { this.port = v; }
            public boolean isSsl()                { return ssl; }
            public void    setSsl(boolean v)      { this.ssl = v; }
            public String  getUsername()          { return username; }
            public void    setUsername(String v)  { this.username = v; }
            public String  getPassword()          { return password; }
            public void    setPassword(String v)  { this.password = v; }
            public String  getFolder()            { return folder; }
            public void    setFolder(String v)    { this.folder = v; }
        }
    }

    private Sessions sessions;
    public Sessions getSessions() { return sessions; }
    public void setSessions(Sessions sessions) { this.sessions = sessions; }

    public static final class Sessions {
        private int maxPerTest = 2;
        public int  getMaxPerTest()        { return maxPerTest; }
        public void setMaxPerTest(int v)   { this.maxPerTest = v; }
    }

    private Performance performance;
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }

    public static final class Performance {
        private boolean captureOnEveryTest = false;
        private double  lcpWarnMs          = 0;   // 0 = disabled
        private double  fcpWarnMs          = 0;
        private double  ttfbWarnMs         = 0;
        private double  clsWarn            = 0;

        public boolean isCaptureOnEveryTest()                { return captureOnEveryTest; }
        public void    setCaptureOnEveryTest(boolean v)      { this.captureOnEveryTest = v; }
        public double  getLcpWarnMs()                        { return lcpWarnMs; }
        public void    setLcpWarnMs(double v)                { this.lcpWarnMs = v; }
        public double  getFcpWarnMs()                        { return fcpWarnMs; }
        public void    setFcpWarnMs(double v)                { this.fcpWarnMs = v; }
        public double  getTtfbWarnMs()                       { return ttfbWarnMs; }
        public void    setTtfbWarnMs(double v)               { this.ttfbWarnMs = v; }
        public double  getClsWarn()                          { return clsWarn; }
        public void    setClsWarn(double v)                  { this.clsWarn = v; }
    }

    private Quarantine quarantine;
    public Quarantine getQuarantine() { return quarantine; }
    public void setQuarantine(Quarantine quarantine) { this.quarantine = quarantine; }

    public static final class Quarantine {
        private boolean enabled      = true;
        private String  cucumberTag  = "quarantine";

        public boolean isEnabled()              { return enabled; }
        public void    setEnabled(boolean v)    { this.enabled = v; }

        /** Tag name to look for in Cucumber {@code .feature} files (without the {@code @} prefix). */
        public String  getCucumberTag()         { return cucumberTag; }
        public void    setCucumberTag(String v) { this.cucumberTag = v != null ? v : "quarantine"; }
    }

    private Clock clock;
    public Clock getClock() { return clock; }
    public void setClock(Clock clock) { this.clock = clock; }

    public static final class Clock {
        private boolean injectHeader = false;
        private String  headerName   = "X-Mock-Date";

        public boolean isInjectHeader()              { return injectHeader; }
        public void    setInjectHeader(boolean v)    { this.injectHeader = v; }
        public String  getHeaderName()               { return headerName; }
        public void    setHeaderName(String v)       { this.headerName = v; }
    }

    private Database database;
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }

    public static final class Database {
        private String url;
        private String username;
        private String password;
        private String driver;
        private java.util.Map<String, DataSource> datasources = new java.util.LinkedHashMap<>();

        public String getUrl()              { return url; }
        public void   setUrl(String v)      { this.url = v; }
        public String getUsername()         { return username; }
        public void   setUsername(String v) { this.username = v; }
        public String getPassword()         { return password; }
        public void   setPassword(String v) { this.password = v; }
        public String getDriver()           { return driver; }
        public void   setDriver(String v)   { this.driver = v; }

        public java.util.Map<String, DataSource> getDatasources() { return datasources; }
        public void setDatasources(java.util.Map<String, DataSource> v) { this.datasources = v != null ? v : new java.util.LinkedHashMap<>(); }

        public static final class DataSource {
            private String url;
            private String username;
            private String password;
            private String driver;

            public String getUrl()              { return url; }
            public void   setUrl(String v)      { this.url = v; }
            public String getUsername()         { return username; }
            public void   setUsername(String v) { this.username = v; }
            public String getPassword()         { return password; }
            public void   setPassword(String v) { this.password = v; }
            public String getDriver()           { return driver; }
            public void   setDriver(String v)   { this.driver = v; }
        }
    }

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public static final class Api {
        private String  baseUrl;
        private int     timeoutSeconds = 30;
        private boolean logBody        = false;
        private boolean logContext     = true;
        private java.util.Map<String, AuthStrategy> auth = new java.util.LinkedHashMap<>();

        public String  getBaseUrl()          { return baseUrl; }
        public void    setBaseUrl(String v)  { this.baseUrl = v; }

        public int     getTimeoutSeconds()         { return timeoutSeconds; }
        public void    setTimeoutSeconds(int v)    { this.timeoutSeconds = v; }

        public boolean isLogBody()                 { return logBody; }
        public void    setLogBody(boolean v)       { this.logBody = v; }

        public boolean isLogContext()              { return logContext; }
        public void    setLogContext(boolean v)    { this.logContext = v; }

        public java.util.Map<String, AuthStrategy> getAuth() { return auth; }
        public void setAuth(java.util.Map<String, AuthStrategy> auth) { this.auth = auth; }

        public static final class AuthStrategy {
            private String type;          // bearer | basic | oauth2
            private String token;         // bearer
            private String username;      // basic
            private String password;      // basic
            private String tokenUrl;      // oauth2
            private String clientId;      // oauth2
            private String clientSecret;  // oauth2

            public String getType()            { return type; }
            public void   setType(String v)    { this.type = v; }

            public String getToken()           { return token; }
            public void   setToken(String v)   { this.token = v; }

            public String getUsername()        { return username; }
            public void   setUsername(String v){ this.username = v; }

            public String getPassword()        { return password; }
            public void   setPassword(String v){ this.password = v; }

            public String getTokenUrl()        { return tokenUrl; }
            public void   setTokenUrl(String v){ this.tokenUrl = v; }

            public String getClientId()        { return clientId; }
            public void   setClientId(String v){ this.clientId = v; }

            public String getClientSecret()        { return clientSecret; }
            public void   setClientSecret(String v){ this.clientSecret = v; }
        }
    }

    // ── Test Management ──────────────────────────────────────────────────────

    public static final class TestManagement {
        private TestRail testrail = new TestRail();
        private Xray     xray     = new Xray();

        public TestRail getTestrail() { return testrail; }
        public void     setTestrail(TestRail v) { this.testrail = v; }

        public Xray  getXray()     { return xray; }
        public void  setXray(Xray v) { this.xray = v; }

        public static final class TestRail {
            private boolean enabled     = false;
            private String  url;
            private String  username;
            private String  apiKey;
            private int     projectId;
            private int     suiteId;
            private String  runName     = "Selenium Boot Run";
            private boolean autoCreateRun = true;
            private int     runId;     // populated at runtime; may be set explicitly to skip creation

            public boolean isEnabled()                { return enabled; }
            public void    setEnabled(boolean v)      { this.enabled = v; }

            public String  getUrl()                   { return url; }
            public void    setUrl(String v)           { this.url = v; }

            public String  getUsername()              { return username; }
            public void    setUsername(String v)      { this.username = v; }

            public String  getApiKey()                { return apiKey; }
            public void    setApiKey(String v)        { this.apiKey = v; }

            public int     getProjectId()             { return projectId; }
            public void    setProjectId(int v)        { this.projectId = v; }

            public int     getSuiteId()               { return suiteId; }
            public void    setSuiteId(int v)          { this.suiteId = v; }

            public String  getRunName()               { return runName; }
            public void    setRunName(String v)       { this.runName = v; }

            public boolean isAutoCreateRun()          { return autoCreateRun; }
            public void    setAutoCreateRun(boolean v){ this.autoCreateRun = v; }

            public int     getRunId()                 { return runId; }
            public void    setRunId(int v)            { this.runId = v; }
        }

        public static final class Xray {
            private boolean enabled      = false;
            /** "cloud" (Xray Cloud / Jira Cloud) or "server" (Xray Server / Data Center). */
            private String  mode         = "cloud";
            // Cloud-only fields
            private String  clientId;
            private String  clientSecret;
            // Server/DC fields
            private String  jiraUrl;
            private String  username;
            private String  password;
            // Shared
            private String  projectKey;
            private String  testPlanKey;

            public boolean isEnabled()                  { return enabled; }
            public void    setEnabled(boolean v)        { this.enabled = v; }

            public String  getMode()                    { return mode; }
            public void    setMode(String v)            { this.mode = v; }

            public String  getClientId()                { return clientId; }
            public void    setClientId(String v)        { this.clientId = v; }

            public String  getClientSecret()            { return clientSecret; }
            public void    setClientSecret(String v)    { this.clientSecret = v; }

            public String  getJiraUrl()                 { return jiraUrl; }
            public void    setJiraUrl(String v)         { this.jiraUrl = v; }

            public String  getUsername()                { return username; }
            public void    setUsername(String v)        { this.username = v; }

            public String  getPassword()                { return password; }
            public void    setPassword(String v)        { this.password = v; }

            public String  getProjectKey()              { return projectKey; }
            public void    setProjectKey(String v)      { this.projectKey = v; }

            public String  getTestPlanKey()             { return testPlanKey; }
            public void    setTestPlanKey(String v)     { this.testPlanKey = v; }
        }
    }

}
