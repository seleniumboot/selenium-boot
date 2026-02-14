package com.seleniumboot.metrics;

public class TestTiming {
    private final String testid;
    private final String threadName;
    private long testExecutionTime;
    private long totalTime;

    public TestTiming(String testid, String threadName) {
        this.testid = testid;
        this.threadName = threadName;
    }

    private long driverStartupTime;

    public String getTestid() {
        return testid;
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
