package com.seleniumboot.driver;

/**
 * Extension of {@link DriverProvider} for use with Java SPI.
 *
 * <p>Implementors declare a browser name that the framework uses to select
 * this provider instead of the built-in Chrome/Firefox providers.
 *
 * <p>Register via:
 * <pre>META-INF/services/com.seleniumboot.driver.NamedDriverProvider</pre>
 *
 * <p>Example — Edge support:
 * <pre>
 * public class EdgeDriverProvider implements NamedDriverProvider {
 *     public String browserName() { return "edge"; }
 *     public WebDriver createDriver() { return new EdgeDriver(); }
 * }
 * </pre>
 */
public interface NamedDriverProvider extends DriverProvider {

    /**
     * The browser name this provider handles, matched case-insensitively
     * against the {@code browser.name} config value.
     */
    String browserName();
}
