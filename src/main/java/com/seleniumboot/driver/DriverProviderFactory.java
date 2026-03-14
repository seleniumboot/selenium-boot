package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

public final class DriverProviderFactory {
    private DriverProviderFactory() {}

    public static DriverProvider getProvider() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();
        String browser = config.getBrowser().getName();
        String executionMode = config.getExecution().getMode();

        if ("remote".equalsIgnoreCase(executionMode)) {
            return new RemoteDriverProvider();
        }

        // Custom providers registered via SPI or programmatically take precedence
        DriverProvider custom = DriverProviderRegistry.find(browser);
        if (custom != null) {
            return custom;
        }

        if ("chrome".equalsIgnoreCase(browser)) {
            return new LocalChromeDriverProvider();
        }

        if ("firefox".equalsIgnoreCase(browser)) {
            return new LocalFirefoxDriverProvider();
        }

        throw new IllegalStateException(
            "Unsupported browser: " + browser +
            ". Register a NamedDriverProvider via SPI or DriverProviderRegistry.register()."
        );
    }
}
