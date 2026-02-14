package com.seleniumboot.driver;

import org.openqa.selenium.WebDriver;

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

        DriverProvider provider = DriverProviderFactory.getProvider();
        WebDriver driver = provider.createDriver();

        if (driver == null) {
            throw new IllegalStateException(
                    "DriverProvider returned null WebDriver"
            );
        }
        DRIVER.set(driver);
    }

    /**
     * Get WebDriver bound to current thread.
     */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialized for current thread. " +
                            "Did you forget to call DriverManager.createDriver()?"
            );
        }
        driver.manage().window().maximize();
        return driver;
    }

    /**
     * Quit and unbind WebDriver from current thread.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception e) {
            System.err.println("[Selenium Boot] Driver quit failed: "
                    + e.getMessage());
        } finally {
            DRIVER.remove();
        }
    }
}
