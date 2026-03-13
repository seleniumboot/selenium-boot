package com.seleniumboot.internal;

import com.seleniumboot.config.SeleniumBootConfig;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SeleniumBootContext holds immutable, framework-wide state.
 * It is initialized once and remains read-only during execution.
 *
 * <p>Thread-safety guarantee: config is published via AtomicReference,
 * ensuring all threads see the fully-constructed object after initialize().
 */
public final class SeleniumBootContext {

    private static final AtomicReference<SeleniumBootConfig> CONFIG = new AtomicReference<>();
    private static final ThreadLocal<String> CURRENT_TEST = new ThreadLocal<>();

    private SeleniumBootContext() {
        // utility class
    }

    public static void initialize(SeleniumBootConfig seleniumBootConfig) {
        if (seleniumBootConfig == null) {
            throw new IllegalArgumentException("SeleniumBootConfig must not be null");
        }
        // compareAndSet ensures exactly one initialization; subsequent calls are no-ops
        CONFIG.compareAndSet(null, seleniumBootConfig);
    }

    // ==========================================================
    // Config
    // ==========================================================

    public static SeleniumBootConfig getConfig() {
        SeleniumBootConfig cfg = CONFIG.get();
        if (cfg == null) {
            throw new IllegalStateException(
                "SeleniumBootContext accessed before framework initialization");
        }
        return cfg;
    }

    public static boolean isInitialized() {
        return CONFIG.get() != null;
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
