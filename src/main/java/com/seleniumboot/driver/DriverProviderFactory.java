package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

public final class DriverProviderFactory {
    public DriverProviderFactory() {}

    public static DriverProvider getProvider() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();
        String browser = config.getBrowser().getName();

        if ("chrome".equalsIgnoreCase(browser)) {
            return new LocalChromeDriverProvider();
        }

        if ("firefox".equalsIgnoreCase(browser)) {
            return new LocalFirefoxDriverProvider();
        }

        throw new IllegalStateException("Unsupported browser: " + browser);
    }
}
