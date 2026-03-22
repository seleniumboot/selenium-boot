package com.seleniumboot.browser;

/**
 * ThreadLocal holder for a per-test browser override injected by {@link BrowserMatrixListener}.
 *
 * <p>When {@code browser.matrix} is configured, each XmlTest clone sets the parameter
 * {@code selenium.boot.browser}. {@link com.seleniumboot.listeners.TestExecutionListener}
 * reads that parameter and stores it here before the WebDriver is created.
 * {@link com.seleniumboot.driver.DriverProviderFactory} then picks it up to select the
 * correct browser provider instead of reading {@code browser.name} from YAML.
 *
 * <p>The value is cleared at the end of every test to prevent leakage across tests.
 */
public final class BrowserContext {

    private static final ThreadLocal<String> BROWSER = new ThreadLocal<>();

    private BrowserContext() {}

    /** Sets the browser override for the current thread. */
    public static void set(String browser) {
        BROWSER.set(browser);
    }

    /**
     * Returns the browser override for the current thread, or {@code null} when no
     * override is active (i.e., the YAML {@code browser.name} should be used instead).
     */
    public static String get() {
        return BROWSER.get();
    }

    /** Clears the browser override for the current thread. Should be called at test end. */
    public static void clear() {
        BROWSER.remove();
    }
}
