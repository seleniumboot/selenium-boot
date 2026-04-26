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

        public boolean isSelfHealing()           { return selfHealing; }
        public void    setSelfHealing(boolean v) { this.selfHealing = v; }
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

}
