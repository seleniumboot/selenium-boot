package com.seleniumboot.assertion;

/**
 * Thread-local holder for the current test's {@link SoftAssertionCollector}.
 * Managed by the framework — do not call directly in test code.
 */
public final class SoftAssertions {

    private static final ThreadLocal<SoftAssertionCollector> COLLECTOR =
            ThreadLocal.withInitial(SoftAssertionCollector::new);

    private SoftAssertions() {}

    /** Returns the collector for the current thread. Never null. */
    public static SoftAssertionCollector get() {
        return COLLECTOR.get();
    }

    /** Clears the collector for the current thread. Called by the framework after each test. */
    public static void clear() {
        COLLECTOR.get().clear();
    }
}
