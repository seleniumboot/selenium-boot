package com.seleniumboot.flakiness;

/** What this run's result means for a test, given its retries and recent history. */
public enum TestStatus {
    /** Failed an attempt in this run, then passed on a retry. */
    PASSED_ON_RETRY("Passed on retry"),
    /** Failed now after passing in every earlier run in the history window. */
    NEWLY_FAILING("Newly failing"),
    /** Failed now and in every earlier run in the history window (at least two of them). */
    CONSISTENTLY_FAILING("Consistently failing"),
    /** Both passes and failures in the earlier runs, so this result is not a surprise either way. */
    INTERMITTENT("Intermittent");

    private final String label;

    TestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
