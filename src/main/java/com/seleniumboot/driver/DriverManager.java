package com.seleniumboot.driver;

import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * DriverManager controls the WebDriver lifecycle.
 *
 * Rules:
 * <li>One WebDriver per thread</li>
 * <li>ThreadLocal ownership</li>
 * <li>Framework-managed creation & destruction only</li>
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = ThreadLocal.withInitial(() -> null);

    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger(0);

    private DriverManager() {
        // utility class
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
            return; // retry-safe
        }

        int maxSessions = SeleniumBootContext.getConfig()
                .getExecution()
                .getMaxActiveSessions();

        int current = ACTIVE_SESSIONS.incrementAndGet();

        if (current > maxSessions) {

            ACTIVE_SESSIONS.decrementAndGet();

            throw new IllegalStateException(
                    "Max active sessions limit reached (" +
                            maxSessions + ")"
            );
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

            // Record driver startup timing
            String testId = SeleniumBootContext.getCurrentTestId();

            if (testId != null) {
                ExecutionMetrics.recordDriverStartup(
                        testId,
                        startupDuration
                );
            }

            System.out.println(
                    "[Selenium Boot] Active sessions: "
                            + ACTIVE_SESSIONS.get()
            );

        } catch (Exception e) {

            ACTIVE_SESSIONS.decrementAndGet();
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
     */
    public static void quitDriver() {

        WebDriver driver = DRIVER.get();

        try {
            if (driver != null) {
                driver.quit();
                ACTIVE_SESSIONS.decrementAndGet();
            }
        } catch (Exception e) {
            System.err.println(
                    "[Selenium Boot] Driver quit failed: " + e.getMessage()
            );
        } finally {
            DRIVER.remove();
        }

        System.out.println(
                "[Selenium Boot] Active sessions: " + ACTIVE_SESSIONS.get()
        );
    }
}
