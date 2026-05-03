package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class or method as browser-free.
 *
 * <p>When present, the framework skips WebDriver creation, recording, console-error
 * collection, screenshot capture, trace recording, and driver teardown for that test.
 * All other framework services (HTML report, step timeline, metrics, retry, CI gates,
 * hooks, test data) continue to work normally.
 *
 * <p>Use this on tests that perform only database assertions, API calls, file
 * operations, or other non-UI work while still extending {@link BaseTest}.
 *
 * <pre>
 * {@literal @}NoBrowser
 * public class DbAssertDemoTest extends BaseTest {
 *
 *     {@literal @}Test
 *     public void assertUserExists() {
 *         db().assertRowExists("users", Map.of("email", "alice@example.com"));
 *     }
 * }
 * </pre>
 *
 * <p>Method-level {@code @NoBrowser} overrides a class without the annotation,
 * allowing a mixed class where only specific tests skip the browser.
 */
@SeleniumBootApi(since = "1.13.0")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoBrowser {
}
