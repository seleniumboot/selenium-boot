package com.seleniumboot.internal;

import com.seleniumboot.config.SeleniumBootConfig;

/**
 * SeleniumBootContext holds immutable, framework-wide state.
 * It is initialized once and remains read-only during execution.
 */
public final class SeleniumBootContext {

    private static volatile boolean initialized = false;
    private static SeleniumBootConfig config;

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
}
