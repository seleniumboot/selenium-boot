package com.seleniumboot.listeners;

import com.seleniumboot.internal.SeleniumBootContext;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Controls retry behavior for failed test methods.
 *
 * Rules:
 * <li>Retry count comes from configuration</li>
 * <li>Retries apply only to test methods</li>
 * <li>No infinite retries</li>
 * <li>Final failure must surface</li>
 */
public final class RetryListener implements IRetryAnalyzer {

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {

        var config = SeleniumBootContext.getConfig();

        if (config.getRetry() == null || !config.getRetry().isEnabled()) {
            return false;
        }

        int maxAttempts = config.getRetry().getMaxAttempts();

        if (attempt < maxAttempts) {
            attempt++;
            return true;
        }

        return false;
    }
}
