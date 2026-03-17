package com.seleniumboot.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method as part of the stable Selenium Boot public API.
 *
 * <h2>Stability contract</h2>
 * <ul>
 *   <li>Types and methods annotated with {@code @SeleniumBootApi} will not be
 *       renamed, removed, or have their signatures changed within the same major version.</li>
 *   <li>Breaking changes to annotated API are only permitted across major version boundaries
 *       (e.g. {@code 1.x → 2.0}).</li>
 *   <li>New methods added to annotated interfaces will always provide a {@code default}
 *       implementation so existing implementations continue to compile.</li>
 * </ul>
 *
 * <h2>What is NOT covered</h2>
 * <ul>
 *   <li>Internal classes (no {@code @SeleniumBootApi}) — may change at any time.</li>
 *   <li>Classes in {@code *.internal.*} packages — always internal, never stable.</li>
 *   <li>Anything marked {@code @Deprecated} — scheduled for removal in the next major version.</li>
 * </ul>
 *
 * @since 0.7.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SeleniumBootApi {

    /**
     * The framework version in which this API element was first introduced.
     * Example: {@code "0.7.0"}
     */
    String since();
}
