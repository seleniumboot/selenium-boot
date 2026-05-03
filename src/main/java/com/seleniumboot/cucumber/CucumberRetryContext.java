package com.seleniumboot.cucumber;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-keyed store for per-scenario retry configuration.
 *
 * <p>Set by {@link CucumberHooks} in {@code @Before} when the scenario has a
 * {@code @retryable} or {@code @retryable=N} tag. Read by
 * {@link com.seleniumboot.listeners.RetryListener} after the scenario finishes,
 * before it decides whether to retry.
 *
 * <p>Keyed by thread ID rather than a ThreadLocal so the value survives past
 * the {@code @After} hook (which runs on the same thread as {@code retry()}).
 * The value is overwritten at the start of each scenario's {@code @Before},
 * so no explicit cleanup is needed.
 */
public final class CucumberRetryContext {

    private static final ConcurrentHashMap<Long, Integer> MAX_RETRIES = new ConcurrentHashMap<>();

    private CucumberRetryContext() {}

    /**
     * Sets the max retry count for the current thread's active scenario.
     * Called from {@code CucumberHooks.@Before}.
     *
     * @param maxRetries number of additional attempts after the first failure
     */
    public static void set(int maxRetries) {
        MAX_RETRIES.put(Thread.currentThread().getId(), maxRetries);
    }

    /**
     * Removes any per-scenario override for the current thread, falling back
     * to the global config in {@code selenium-boot.yml}.
     */
    public static void clear() {
        MAX_RETRIES.remove(Thread.currentThread().getId());
    }

    /**
     * Returns the per-scenario max retries for the current thread,
     * or {@code -1} if no tag override is set (use global config).
     */
    public static int get() {
        return MAX_RETRIES.getOrDefault(Thread.currentThread().getId(), -1);
    }
}
