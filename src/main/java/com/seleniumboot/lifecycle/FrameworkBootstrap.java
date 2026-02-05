package com.seleniumboot.lifecycle;

import com.seleniumboot.config.ConfigurationLoader;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

/**
 * FrameworkBootstrap is responsible for initializing Selenium Boot
 * before any TestNG execution begins.
 *
 * This class must be invoked exactly once per test suite.
 */
public final class FrameworkBootstrap {

    private FrameworkBootstrap() {
        // utility class
    }

    public static void initialize() {
        if (SeleniumBootContext.isInitialized()) {
            return;
        }

        SeleniumBootConfig config = ConfigurationLoader.load();
        SeleniumBootContext.initialize(config);
    }
}
