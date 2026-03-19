package com.seleniumboot.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Programmatic overrides for Selenium Boot config defaults.
 *
 * <p>Values set here are applied <em>after</em> YAML loading but only for
 * fields whose YAML value is {@code null} or zero (i.e. not explicitly set
 * by the user's config file). This means YAML always wins over defaults
 * registered here.
 *
 * <p>Typical use: a shared test-base JAR that establishes org-wide defaults,
 * which individual projects can override via their own {@code selenium-boot.yml}.
 *
 * <pre>
 * // In a @BeforeSuite that runs before FrameworkBootstrap:
 * SeleniumBootDefaults.set("browser.name", "edge");
 * SeleniumBootDefaults.set("timeouts.explicit", 15);
 * SeleniumBootDefaults.set("execution.maxActiveSessions", 10);
 * </pre>
 *
 * <h3>Supported keys</h3>
 * <ul>
 *   <li>{@code browser.name} — String</li>
 *   <li>{@code browser.headless} — Boolean (only applied when YAML omits the field — primitives default false)</li>
 *   <li>{@code timeouts.explicit} — Integer (seconds)</li>
 *   <li>{@code timeouts.pageLoad} — Integer (seconds)</li>
 *   <li>{@code execution.maxActiveSessions} — Integer</li>
 *   <li>{@code execution.threadCount} — Integer</li>
 *   <li>{@code retry.enabled} — Boolean</li>
 *   <li>{@code retry.maxAttempts} — Integer</li>
 * </ul>
 */
public final class SeleniumBootDefaults {

    private static final Map<String, Object> overrides = new ConcurrentHashMap<>();

    private SeleniumBootDefaults() {}

    /** Sets a default value for the given config key. */
    public static void set(String key, Object value) {
        overrides.put(key, value);
    }

    /** Returns the registered default for the given key, or {@code null} if none. */
    public static Object get(String key) {
        return overrides.get(key);
    }

    /** Clears all registered defaults (useful in tests). */
    public static void reset() {
        overrides.clear();
    }

    /**
     * Applies registered defaults to {@code config} for any fields that are
     * {@code null} or zero (i.e. not set by YAML). Called by
     * {@link com.seleniumboot.lifecycle.FrameworkBootstrap} after config is loaded.
     */
    public static void applyMissing(SeleniumBootConfig config) {
        applyBrowserDefaults(config);
        applyTimeoutDefaults(config);
        applyExecutionDefaults(config);
        applyRetryDefaults(config);
    }

    private static void applyBrowserDefaults(SeleniumBootConfig config) {
        SeleniumBootConfig.Browser browser = config.getBrowser();
        if (browser == null) return;

        if (browser.getName() == null) {
            String name = (String) overrides.get("browser.name");
            if (name != null) browser.setName(name);
        }
    }

    private static void applyTimeoutDefaults(SeleniumBootConfig config) {
        SeleniumBootConfig.Timeouts timeouts = config.getTimeouts();
        if (timeouts == null) return;

        if (timeouts.getExplicit() == 0) {
            Integer val = (Integer) overrides.get("timeouts.explicit");
            if (val != null) timeouts.setExplicit(val);
        }
        if (timeouts.getPageLoad() == 0) {
            Integer val = (Integer) overrides.get("timeouts.pageLoad");
            if (val != null) timeouts.setPageLoad(val);
        }
    }

    private static void applyExecutionDefaults(SeleniumBootConfig config) {
        SeleniumBootConfig.Execution execution = config.getExecution();
        if (execution == null) return;

        if (execution.getMaxActiveSessions() == 0) {
            Integer val = (Integer) overrides.get("execution.maxActiveSessions");
            if (val != null) execution.setMaxActiveSessions(val);
        }
        if (execution.getThreadCount() == 0) {
            Integer val = (Integer) overrides.get("execution.threadCount");
            if (val != null) execution.setThreadCount(val);
        }
    }

    private static void applyRetryDefaults(SeleniumBootConfig config) {
        SeleniumBootConfig.Retry retry = config.getRetry();
        if (retry == null) return;

        if (retry.getRawMaxAttempts() == null) {
            Integer val = (Integer) overrides.get("retry.maxAttempts");
            if (val != null) retry.setMaxAttempts(val);
        }
    }
}
