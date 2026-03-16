package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * DriverManager controls the WebDriver lifecycle.
 *
 * Rules:
 * <li>One WebDriver per thread</li>
 * <li>ThreadLocal ownership</li>
 * <li>Framework-managed creation & destruction only</li>
 * <li>Session limit is enforced with a blocking Semaphore — tests wait for a slot
 *     rather than failing fast, preventing spurious failures under parallel load</li>
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = ThreadLocal.withInitial(() -> null);

    /** Tracks all drivers created under per-suite lifecycle for bulk teardown. */
    private static final java.util.Set<WebDriver> SUITE_DRIVERS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Lazy-initialized from config; null until first createDriver() call. */
    private static volatile Semaphore SESSION_SEMAPHORE;
    private static volatile int MAX_SESSIONS;

    private static Semaphore getOrInitSemaphore() {
        if (SESSION_SEMAPHORE == null) {
            synchronized (DriverManager.class) {
                if (SESSION_SEMAPHORE == null) {
                    MAX_SESSIONS = SeleniumBootContext.getConfig()
                            .getExecution()
                            .getMaxActiveSessions();
                    SESSION_SEMAPHORE = new Semaphore(MAX_SESSIONS, true); // fair
                }
            }
        }
        return SESSION_SEMAPHORE;
    }

    private static int activeSessions() {
        return SESSION_SEMAPHORE == null ? 0 : MAX_SESSIONS - SESSION_SEMAPHORE.availablePermits();
    }

    private DriverManager() {}

    // ==========================================================
    // Lifecycle helpers
    // ==========================================================

    private static boolean isPerSuite() {
        try {
            SeleniumBootConfig.Browser b = SeleniumBootContext.getConfig().getBrowser();
            return b != null && "per-suite".equalsIgnoreCase(b.getLifecycle());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} when the framework should quit the driver after
     * each test method (default {@code per-test} lifecycle).
     * {@code false} means the driver is kept alive until suite end.
     */
    public static boolean shouldQuitAfterTest() {
        return !isPerSuite();
    }

    // ==========================================================
    // Driver Creation
    // ==========================================================

    /**
     * Create and bind WebDriver to current thread.
     * Idempotent — safe for retry scenarios.
     */
    public static void createDriver() {

        if (DRIVER.get() != null) {
            return; // retry-safe — semaphore permit already held by this thread
        }

        Semaphore semaphore = getOrInitSemaphore();

        try {
            boolean acquired = semaphore.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException(
                        "Timed out waiting for an available session slot after 30s. " +
                        "Consider increasing maxActiveSessions in configuration."
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a session slot", e);
        }

        try {

            long startTime = System.currentTimeMillis();

            DriverProvider provider =
                    DriverProviderFactory.getProvider();

            WebDriver driver = provider.createDriver();

            if (driver == null) {
                throw new IllegalStateException(
                        "DriverProvider returned null WebDriver"
                );
            }

            long startupDuration =
                    System.currentTimeMillis() - startTime;

            DRIVER.set(driver);

            // Register in suite-driver registry when lifecycle is per-suite
            if (isPerSuite()) {
                SUITE_DRIVERS.add(driver);
                System.out.println("[Selenium Boot] Browser lifecycle: per-suite — driver will be reused across tests on this thread.");
            }

            // Record driver startup timing
            String testId = SeleniumBootContext.getCurrentTestId();

            if (testId != null) {
                ExecutionMetrics.recordDriverStartup(
                        testId,
                        startupDuration
                );
            }

            System.out.println("[Selenium Boot] Active sessions: " + activeSessions());

        } catch (Exception e) {
            semaphore.release(); // return the permit — driver was never stored
            throw e;
        }
    }

    // ==========================================================
    // Driver Access
    // ==========================================================

    /**
     * Get WebDriver bound to current thread.
     * Performs health check before returning.
     */
    public static WebDriver getDriver() {

        WebDriver driver = DRIVER.get();

        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialized for current thread."
            );
        }

        if (!isDriverAlive()) {

            System.err.println(
                    "[Selenium Boot] Driver session invalid. Recreating..."
            );

            recreateDriver();
            driver = DRIVER.get();
        }

        return driver;
    }

    // ==========================================================
    // Driver Recreation (Self-Healing)
    // ==========================================================

    public static void recreateDriver() {

        try {
            quitDriver();
        } catch (Exception ignored) {
        }

        createDriver();
    }

    /**
     * Lightweight session health check.
     */
    public static boolean isDriverAlive() {

        WebDriver driver = DRIVER.get();

        if (driver == null) {
            return false;
        }

        try {
            driver.getTitle(); // lightweight call
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================================
    // Driver Teardown
    // ==========================================================

    /**
     * Quit and unbind WebDriver from current thread.
     * No-op when lifecycle is {@code per-suite} — driver stays alive until
     * {@link #quitAllSuiteDrivers()} is called at suite end.
     */
    public static void quitDriver() {

        if (isPerSuite()) {
            return; // driver intentionally kept alive for next test on this thread
        }

        WebDriver driver = DRIVER.get();

        try {
            if (driver != null) {
                driver.quit();
                getOrInitSemaphore().release();
            }
        } catch (Exception e) {
            System.err.println("[Selenium Boot] Driver quit failed: " + e.getMessage());
        } finally {
            DRIVER.remove();
        }

        System.out.println("[Selenium Boot] Active sessions: " + activeSessions());
    }

    /**
     * Quits all drivers tracked under {@code per-suite} lifecycle and releases
     * their semaphore permits. Called once by {@code SuiteExecutionListener.onFinish}.
     * Safe to call in {@code per-test} mode — no-op when registry is empty.
     */
    public static void quitAllSuiteDrivers() {
        if (SUITE_DRIVERS.isEmpty()) return;
        int released = 0;
        for (WebDriver driver : SUITE_DRIVERS) {
            try {
                driver.quit();
                released++;
            } catch (Exception e) {
                System.err.println("[Selenium Boot] Error quitting suite driver: " + e.getMessage());
                released++; // release permit regardless — session is gone
            }
        }
        if (SESSION_SEMAPHORE != null) {
            SESSION_SEMAPHORE.release(released);
        }
        SUITE_DRIVERS.clear();
        DRIVER.remove();
        System.out.println("[Selenium Boot] All suite drivers quit. Released " + released + " session slot(s).");
    }
}
