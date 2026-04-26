package com.seleniumboot.flakiness;

/**
 * Flakiness risk score for a single test, computed across multiple historical runs.
 */
public final class FlakinessScore {

    public enum Risk { STABLE, WATCH, HIGH }

    private final String testId;
    private final int    runsAnalysed;
    private final int    failCount;
    private final double failureRate;   // 0–100
    private final Risk   risk;

    public FlakinessScore(String testId, int runsAnalysed, int failCount,
                          double failureRate, Risk risk) {
        this.testId       = testId;
        this.runsAnalysed = runsAnalysed;
        this.failCount    = failCount;
        this.failureRate  = failureRate;
        this.risk         = risk;
    }

    public String getTestId()       { return testId; }
    public int    getRunsAnalysed() { return runsAnalysed; }
    public int    getFailCount()    { return failCount; }
    public double getFailureRate()  { return failureRate; }
    public Risk   getRisk()         { return risk; }

    public static Risk classify(double failureRate, double highThreshold) {
        if (failureRate >= highThreshold) return Risk.HIGH;
        if (failureRate >= 10.0)          return Risk.WATCH;
        return Risk.STABLE;
    }
}
