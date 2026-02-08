package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * DriverManager controls the WebDriver lifecycle.
 *
 * Rules:
 * - One WebDriver per thread
 * - ThreadLocal ownership
 * - Framework-managed creation & destruction only
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

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

        SeleniumBootConfig config = SeleniumBootContext.getConfig();
        String browserName = config.getBrowser().getName();

        if (!"chrome".equalsIgnoreCase(browserName)) {
            throw new IllegalStateException(
                    "Unsupported browser for MVP: " + browserName
            );
        }

        ChromeOptions options = new ChromeOptions();

        if (config.getBrowser().isHeadless()) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
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
            // Swallow quit exceptions to avoid blocking execution
        } finally {
            DRIVER.remove();
        }
    }
}
