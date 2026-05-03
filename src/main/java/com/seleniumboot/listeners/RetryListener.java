package com.seleniumboot.listeners;

import com.seleniumboot.internal.SeleniumBootContext;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.lang.reflect.Method;

/**
 * Controls retry behavior for failed test methods.
 *
 * <p>Decision logic (evaluated in order):
 * <ol>
 *   <li>{@code retry.enabled=false} in config → never retry (global kill switch)</li>
 *   <li>{@code retry.enabled=true} → retry ALL tests up to {@code maxAttempts}</li>
 *   <li>Method is annotated with {@link Retryable} → retry up to {@code maxAttempts}
 *       (allows per-method opt-in when global retry is off)</li>
 * </ol>
 *
 * <p>Rules:
 * <li>Retry count comes from configuration</li>
 * <li>Retries apply only to test methods</li>
 * <li>No infinite retries — final failure always surfaces</li>
 */
public final class RetryListener implements IRetryAnalyzer {

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {

        // Guard: framework may not be initialized when running unit tests
        if (!SeleniumBootContext.isInitialized()) {
            return false;
        }

        var retryConfig = SeleniumBootContext.getConfig().getRetry();

        // Master kill switch — if retry is disabled nothing retries, including @Retryable
        if (retryConfig == null || !retryConfig.isEnabled()) {
            return false;
        }

        // Global mode: all tests retry. Method-level: only @Retryable tests retry.
        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        boolean isGlobalRetry = retryConfig.isEnabled();
        boolean isAnnotated = method != null && method.isAnnotationPresent(Retryable.class);

        if (!isGlobalRetry && !isAnnotated) {
            return false;
        }

        // Per-method @Retryable.maxAttempts overrides global config
        int maxAttempts = retryConfig.getMaxAttempts();
        if (isAnnotated) {
            Retryable annotation = method.getAnnotation(Retryable.class);
            if (annotation != null && annotation.maxAttempts() >= 0) {
                maxAttempts = annotation.maxAttempts();
            }
        }

        // Cucumber @retryable=N tag overrides everything (set by CucumberHooks)
        try {
            int cucumberOverride = com.seleniumboot.cucumber.CucumberRetryContext.get();
            if (cucumberOverride >= 0) {
                maxAttempts = cucumberOverride;
            }
        } catch (Exception ignored) {
            // CucumberRetryContext unavailable (non-Cucumber run) — use resolved value
        }

        if (attempt < maxAttempts) {
            attempt++;
            return true;
        }

        return false;
    }
}
