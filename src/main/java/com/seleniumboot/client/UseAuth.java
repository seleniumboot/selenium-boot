package com.seleniumboot.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a named auth strategy (defined in {@code selenium-boot.yml}) to a test method or class.
 * The framework resolves the strategy and calls {@link ApiClient#setGlobalAuth} before the test runs.
 *
 * <pre>
 * # selenium-boot.yml
 * api:
 *   auth:
 *     adminToken:
 *       type: bearer
 *       token: ${ADMIN_TOKEN}
 *     serviceAccount:
 *       type: oauth2
 *       tokenUrl: https://auth.example.com/token
 *       clientId: ${CLIENT_ID}
 *       clientSecret: ${CLIENT_SECRET}
 *
 * // Test
 * {@literal @}Test
 * {@literal @}UseAuth("adminToken")
 * public void createUser() {
 *     apiClient().post("/api/users").body(...).send().assertStatus(201);
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface UseAuth {
    /** Name of the auth strategy in {@code api.auth} config block. */
    String value();
}
