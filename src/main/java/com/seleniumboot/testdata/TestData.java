package com.seleniumboot.testdata;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares test data to inject for a test method or class.
 *
 * <p>The framework loads the file from {@code src/test/resources/testdata/}.
 * Supports {@code .json} and {@code .yml} / {@code .yaml} files.
 *
 * <p>Environment-specific override: if {@code -Denv=staging} is set and
 * {@code users/admin.staging.json} exists, it is used instead of {@code users/admin.json}.
 *
 * <pre>
 * &#64;Test
 * &#64;TestData("users/admin.json")
 * public void adminCanLogin() {
 *     Map&lt;String, Object&gt; data = getTestData();
 *     String username = (String) data.get("username");
 * }
 * </pre>
 *
 * <p>Class-level annotation acts as default for all methods in the class;
 * a method-level annotation overrides the class-level one.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SeleniumBootApi(since = "1.0.0")
public @interface TestData {
    /** Relative path inside {@code src/test/resources/testdata/}, e.g. {@code "users/admin.json"}. */
    String value();
}
