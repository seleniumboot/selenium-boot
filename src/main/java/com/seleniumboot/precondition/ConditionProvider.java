package com.seleniumboot.precondition;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link BaseConditions} subclass as the provider for a named pre-condition.
 *
 * <p>The method will be called by the framework when a test annotated with
 * {@code @PreCondition("name")} requires that condition and no valid cached session exists.
 *
 * <p>Provider methods must:
 * <ul>
 *   <li>Have no parameters</li>
 *   <li>Return void</li>
 *   <li>Be {@code public}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * public class AppConditions extends BaseConditions {
 *
 *     {@literal @}ConditionProvider("login")
 *     public void login() {
 *         open("/login");
 *         new LoginPage(getDriver()).login("admin", "secret");
 *         // framework caches cookies + localStorage after this returns
 *     }
 *
 *     {@literal @}ConditionProvider("acceptCookies")
 *     public void acceptCookies() {
 *         open("/");
 *         click(By.id("accept-all-cookies"));
 *     }
 * }
 * </pre>
 *
 * @see PreCondition
 * @see BaseConditions
 * @since 0.8.0
 */
@SeleniumBootApi(since = "0.8.0")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionProvider {

    /** The condition name this method satisfies. Must match {@link PreCondition#value()}. */
    String value();
}
