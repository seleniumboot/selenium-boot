package com.seleniumboot.internal;

import com.seleniumboot.config.SeleniumBootConfig;

/**
 * SeleniumBootContext holds immutable, framework-wide state.
 * It is initialized once and remains read-only during execution.
 */
public final class SeleniumBootContext {

    private static volatile boolean initialized = false;
    private static SeleniumBootConfig config;
    private static final ThreadLocal<String> CURRENT_TEST = new ThreadLocal<>();

    private SeleniumBootContext() {
        // utility class
    }

    public static synchronized void initialize(SeleniumBootConfig seleniumBootConfig) {
        if (initialized) {
            return;
        }

        config = seleniumBootConfig;
        initialized = true;
    }

    // ==========================================================
    // Config
    // ==========================================================

    public static void setConfig(SeleniumBootConfig config) {
        SeleniumBootContext.config = config;
    }

    public static SeleniumBootConfig getConfig() {
        if (!initialized) {
            throw new IllegalStateException(
                "SeleniumBootContext accessed before framework initialization");
        }
        return config;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    // ==========================================================
    // Current Test Tracking (Per Thread)
    // ==========================================================

    public static void setCurrentTestId(String testId) {
        CURRENT_TEST.set(testId);
    }

    public static String getCurrentTestId() {
        return CURRENT_TEST.get();
    }

    public static void clearCurrentTestId() {
        CURRENT_TEST.remove();
    }
}
