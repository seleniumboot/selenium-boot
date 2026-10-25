package com.seleniumboot.metrics;

public class TestTiming {
    private final String testId;
    private final String threadName;
    private long testExecutionTime;
    private long totalTime;
    private String status;
    private String screenshotPath;

    public TestTiming(String testId, String threadName) {
        this.testId = testId;
        this.threadName = threadName;
    }

    private long driverStartupTime;
    private int retryCount;
    private String errorMessage;
    private String stackTrace;
    private String testClassName;

    public String getTestId() {
        return testId;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getDriverStartupTime() {
        return driverStartupTime;
    }

    public void setDriverStartupTime(long driverStartupTime) {
        this.driverStartupTime = driverStartupTime;
    }

    public long getTestExecutionTime() {
        return testExecutionTime;
    }

    public void setTestExecutionTime(long testExecutionTime) {
        this.testExecutionTime = testExecutionTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getTestClassName() { return testClassName; }
    public void setTestClassName(String testClassName) { this.testClassName = testClassName; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private String browser;
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    private String recordingPath;
    public String getRecordingPath() { return recordingPath; }
    public void setRecordingPath(String recordingPath) { this.recordingPath = recordingPath; }

    private String tracePath;
    public String getTracePath() { return tracePath; }
    public void setTracePath(String tracePath) { this.tracePath = tracePath; }

    private String failingLocator;
    public String getFailingLocator() { return failingLocator; }
    public void setFailingLocator(String failingLocator) { this.failingLocator = failingLocator; }

    private String pageUrl;
    private String pageTitle;
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

    private final java.util.List<String> consoleErrors =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    public void setConsoleErrors(java.util.List<String> values) {
        consoleErrors.clear();
        if (values != null) consoleErrors.addAll(values);
    }
    public java.util.List<String> getConsoleErrors() {
        return java.util.Collections.unmodifiableList(consoleErrors);
    }

    private int healedCount;
    public int  getHealedCount()  { return healedCount; }
    public void incrementHealed() { this.healedCount++; }

    private String aiAnalysis;
    public String getAiAnalysis()             { return aiAnalysis; }
    public void   setAiAnalysis(String v)     { this.aiAnalysis = v; }

    private String sessionUrl;
    public String getSessionUrl()             { return sessionUrl; }
    public void   setSessionUrl(String v)     { this.sessionUrl = v; }

    private com.seleniumboot.performance.PerformanceMetrics performanceMetrics;
    public com.seleniumboot.performance.PerformanceMetrics getPerformanceMetrics()  { return performanceMetrics; }
    public void setPerformanceMetrics(com.seleniumboot.performance.PerformanceMetrics v) { this.performanceMetrics = v; }

    private final java.util.List<com.seleniumboot.steps.StepRecord> steps =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addStep(com.seleniumboot.steps.StepRecord step) { steps.add(step); }
    public void clearSteps() { steps.clear(); locatorUses.clear(); consoleEntries.clear(); networkFailures.clear(); }

    private static final int MAX_LOCATOR_USES = 1000;
    private final java.util.List<com.seleniumboot.steps.LocatorUse> locatorUses =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    public void addLocatorUse(com.seleniumboot.steps.LocatorUse use) {
        if (locatorUses.size() < MAX_LOCATOR_USES) locatorUses.add(use);
    }

    private static final int MAX_CONSOLE_ENTRIES = 500;
    private static final int MAX_NETWORK_FAILURES = 200;
    private final java.util.List<com.seleniumboot.steps.ConsoleEntry> consoleEntries =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<com.seleniumboot.steps.NetworkFailure> networkFailures =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    public void addConsoleEntry(com.seleniumboot.steps.ConsoleEntry e) {
        if (consoleEntries.size() < MAX_CONSOLE_ENTRIES) consoleEntries.add(e);
    }
    public java.util.List<com.seleniumboot.steps.ConsoleEntry> getConsoleEntries() {
        return java.util.Collections.unmodifiableList(consoleEntries);
    }
    public void addNetworkFailure(com.seleniumboot.steps.NetworkFailure f) {
        if (networkFailures.size() < MAX_NETWORK_FAILURES) networkFailures.add(f);
    }
    public java.util.List<com.seleniumboot.steps.NetworkFailure> getNetworkFailures() {
        return java.util.Collections.unmodifiableList(networkFailures);
    }

    public java.util.List<com.seleniumboot.steps.LocatorUse> getLocatorUses() {
        return java.util.Collections.unmodifiableList(locatorUses);
    }
    public java.util.List<com.seleniumboot.steps.StepRecord> getSteps() {
        return java.util.Collections.unmodifiableList(steps);
    }

    private final java.util.List<String> tags =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public void setTags(java.util.Collection<String> values) {
        tags.clear();
        if (values != null) {
            java.util.LinkedHashSet<String> deduplicated = new java.util.LinkedHashSet<>();
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    String normalized = v.trim();
                    if (normalized.startsWith("@")) {
                        normalized = normalized.substring(1).trim();
                    }
                    if (!normalized.isEmpty()) {
                        deduplicated.add(normalized);
                    }
                }
            }
            tags.addAll(deduplicated);
        }
    }

    public java.util.List<String> getTags() {
        return java.util.Collections.unmodifiableList(tags);
    }
}
