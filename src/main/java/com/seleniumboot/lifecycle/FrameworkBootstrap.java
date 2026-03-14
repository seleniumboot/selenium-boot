package com.seleniumboot.lifecycle;

import com.seleniumboot.config.ConfigurationLoader;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.config.SeleniumBootDefaults;
import com.seleniumboot.driver.DriverProviderRegistry;
import com.seleniumboot.execution.ExecutionValidator;
import com.seleniumboot.extension.PluginRegistry;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.reporting.ReportAdapterRegistry;

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
        SeleniumBootDefaults.applyMissing(config);
        ExecutionValidator.validate(config.getExecution());

        SeleniumBootContext.initialize(config);

        // Load all SPI-registered extension points
        DriverProviderRegistry.loadAll();
        HookRegistry.loadAll();
        ReportAdapterRegistry.loadAll();
        PluginRegistry.loadAll(config);
    }
}
