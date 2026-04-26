package com.seleniumboot.healing;

/**
 * Records one successful locator heal — the original failing {@code By} description,
 * the fallback strategy that succeeded, and which test it happened in.
 */
public final class HealEvent {

    private final String testId;
    private final String originalLocator;
    private final String healedLocator;
    private final String strategy;
    private final long   timestamp;

    public HealEvent(String testId, String originalLocator, String healedLocator, String strategy) {
        this.testId          = testId;
        this.originalLocator = originalLocator;
        this.healedLocator   = healedLocator;
        this.strategy        = strategy;
        this.timestamp       = System.currentTimeMillis();
    }

    public String getTestId()          { return testId; }
    public String getOriginalLocator() { return originalLocator; }
    public String getHealedLocator()   { return healedLocator; }
    public String getStrategy()        { return strategy; }
    public long   getTimestamp()       { return timestamp; }
}
