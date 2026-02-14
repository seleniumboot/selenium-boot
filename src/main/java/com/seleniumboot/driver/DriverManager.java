package com.seleniumboot.driver;

import com.seleniumboot.internal.SeleniumBootContext;
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

    private static final ThreadLocal<WebDriver> DRIVER = ThreadLocal.withInitial(()-> null);
    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger(0);

    private DriverManager() {
        // utility class
    }

    /**
     * Create and bind WebDriver to current thread.
     * Safe to call multiple times (idempotent).
     */
    public static void createDriver() {
        if (DRIVER.get() != null) {
            return;
        }

        int maxSessions = SeleniumBootContext.getConfig().getExecution().getMaxActiveSessions();
        int current = ACTIVE_SESSIONS.incrementAndGet();

        if (current > maxSessions) {
            ACTIVE_SESSIONS.decrementAndGet();

            throw new IllegalStateException(
                    "Max active sessions limit reached (" +
                            maxSessions + ")"
            );
        }

        try {
                DriverProvider provider = DriverProviderFactory.getProvider();
                WebDriver driver = provider.createDriver();

                if (driver == null) {
                    throw new IllegalStateException(
                            "DriverProvider returned null WebDriver"
                    );
                }
                DRIVER.set(driver);

                System.out.println(
                        "[Selenium Boot] Active sessions: "+ ACTIVE_SESSIONS.get()
                );
        } catch (Exception e) {
            ACTIVE_SESSIONS.decrementAndGet();
            throw e;
        }
    }

    /**
     * Get WebDriver bound to current thread.
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

        driver.manage().window().maximize();
        return driver;
    }

    public static void recreateDriver() {
        try {
            quitDriver();
        } catch (Exception ignored) {}

        createDriver();
    }

    public static boolean isDriverAlive() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            return false;
        }
        try {
            driver.getTitle();
            return true;
        }  catch (Exception e) {
            return false;
        }
    }

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
            System.err.println("[Selenium Boot] Driver quit failed: "
                    + e.getMessage());
        } finally {
            DRIVER.remove();
        }

        System.out.println(
                "[Selenium Boot] Active sessions: " + ACTIVE_SESSIONS.get()
        );
    }
}
