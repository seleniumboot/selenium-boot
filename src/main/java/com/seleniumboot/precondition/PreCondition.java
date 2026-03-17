package com.seleniumboot.precondition;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more named pre-conditions that must be satisfied before a test runs.
 *
 * <p>The framework looks up a method annotated with {@link ConditionProvider} matching
 * each name, executes it if needed, and caches the resulting session state (cookies +
 * localStorage). Subsequent tests with the same condition reuse the cached session
 * instead of repeating the setup.
 *
 * <p>Example — eliminating login boilerplate:
 * <pre>
 * {@literal @}Test
 * {@literal @}PreCondition("login")
 * public void viewDashboard() {
 *     open("/dashboard");
 *     Assert.assertTrue(dashboardPage.isLoaded());
 * }
 *
 * {@literal @}Test
 * {@literal @}PreCondition("login")
 * public void editProfile() {
 *     open("/profile");
 *     // already logged in — no @BeforeMethod needed
 * }
 * </pre>
 *
 * <p>Multiple conditions per test are supported:
 * <pre>
 * {@literal @}PreCondition({"login", "acceptCookies"})
 * </pre>
 *
 * @see ConditionProvider
 * @see BaseConditions
 * @since 0.8.0
 */
@SeleniumBootApi(since = "0.8.0")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreCondition {

    /**
     * One or more condition names. Each name must match a
     * {@link ConditionProvider#value()} declared in a registered {@link BaseConditions}.
     */
    String[] value();
}
