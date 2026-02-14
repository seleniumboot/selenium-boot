package com.seleniumboot.metrics;

public class TestTiming {
    private final String testId;
    private final String threadName;
    private long testExecutionTime;
    private long totalTime;

    public TestTiming(String testId, String threadName) {
        this.testId = testId;
        this.threadName = threadName;
    }

    private long driverStartupTime;

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
}
