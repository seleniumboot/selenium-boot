package com.seleniumboot.junit5;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/**
 * Optional JUnit 5 base class — the equivalent of {@code BaseTest} for TestNG users.
 *
 * <p>Extend this class to get driver access and convenience navigation methods
 * without needing to interact with {@link DriverManager} directly.
 *
 * <pre>{@code
 * class LoginTest extends BaseJUnit5Test {
 *     @Test
 *     void loginRedirectsToHome() {
 *         open();
 *         // getDriver() is available from here
 *     }
 * }
 * }</pre>
 *
 * <p>Alternatively, annotate your own base class with {@link EnableSeleniumBoot}
 * and access the driver via {@link DriverManager#getDriver()} directly.
 */
@ExtendWith(SeleniumBootExtension.class)
public abstract class BaseJUnit5Test {

    /**
     * Returns the WebDriver bound to the current thread.
     * Valid only inside a test method — do not call in {@code @BeforeAll}.
     */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Navigates to the {@code execution.baseUrl} defined in {@code selenium-boot.yml}.
     */
    protected void open() {
        getDriver().get(SeleniumBootContext.getConfig().getExecution().getBaseUrl());
    }

    /**
     * Navigates to {@code baseUrl + "/" + path}.
     *
     * @param path relative path, e.g. {@code "login"} or {@code "/dashboard"}
     */
    protected void open(String path) {
        String baseUrl = SeleniumBootContext.getConfig().getExecution().getBaseUrl();
        String separator = baseUrl.endsWith("/") || path.startsWith("/") ? "" : "/";
        getDriver().get(baseUrl + separator + path);
    }
}
