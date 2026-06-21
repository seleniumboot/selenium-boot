package com.seleniumboot.testmanagement;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a test method or class to one or more TestRail case IDs.
 *
 * <pre>{@code
 * @Test
 * @TestRailCase("C1234")
 * public void loginTest() { ... }
 *
 * // Multiple cases
 * @Test
 * @TestRailCase({"C1234", "C5678"})
 * public void checkoutTest() { ... }
 * }</pre>
 *
 * <p>Case IDs may include or omit the leading "C" — both "C1234" and "1234" are accepted.
 */
@SeleniumBootApi(since = "3.0.0")
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestRailCase {
    /** One or more TestRail case IDs (e.g. "C1234" or "1234"). */
    String[] value();
}
