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

    private final java.util.List<com.seleniumboot.steps.StepRecord> steps =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addStep(com.seleniumboot.steps.StepRecord step) { steps.add(step); }
    public void clearSteps() { steps.clear(); }
    public java.util.List<com.seleniumboot.steps.StepRecord> getSteps() {
        return java.util.Collections.unmodifiableList(steps);
    }
}
