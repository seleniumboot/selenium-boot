package com.seleniumboot.listeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method as eligible for retry on failure.
 *
 * Retry count is controlled by {@code retry.maxAttempts} in selenium-boot.yml.
 * Retry can be globally disabled via {@code retry.enabled: false} regardless
 * of this annotation being present.
 *
 * Usage:
 * <pre>
 *   {@literal @}Retryable
 *   {@literal @}Test
 *   public void flakyTest() { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
}