package com.seleniumboot.driver;

import org.openqa.selenium.remote.AbstractDriverOptions;

/**
 * Applies {@code browser.version} to local driver options. Selenium Manager resolves
 * (and downloads if needed) the matching browser build. Omitted or blank leaves the options
 * untouched, so the browser installed on the machine is used.
 */
final class BrowserVersionPin {

    private BrowserVersionPin() {}

    static void apply(AbstractDriverOptions<?> options, String version) {
        if (version != null && !version.isBlank()) {
            options.setBrowserVersion(version.trim());
        }
    }
}
