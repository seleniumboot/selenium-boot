package com.seleniumboot.listeners;

import com.seleniumboot.lifecycle.FrameworkBootstrap;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * SuiteExecutionListener is responsible for framework initialization
 * before any tests are executed.
 *
 * This listener must run exactly once per TestNG suite.
 */
public final class SuiteExecutionListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        try {
            FrameworkBootstrap.initialize();
        } catch (Exception e) {
            // Abort entire suite on bootstrap failure
            throw new IllegalStateException(
                "Selenium Boot failed to initialize. Aborting test suite execution.", e);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        // Reserved for future framework-wide cleanup
    }
}
