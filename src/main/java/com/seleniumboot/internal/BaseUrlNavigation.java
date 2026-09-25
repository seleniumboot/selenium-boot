package com.seleniumboot.internal;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.util.List;

/**
 * Shared by every {@code open()} in the base classes: requires {@code execution.baseUrl} and,
 * when the browser cannot reach it, says so in terms of the config key instead of a raw
 * {@code net::ERR_NAME_NOT_RESOLVED}.
 */
public final class BaseUrlNavigation {

    static final String MISSING_BASE_URL =
            "execution.baseUrl is not set — add it to selenium-boot.yml, "
            + "or navigate with getDriver().get(url)";

    // Chrome/Edge net errors, Firefox error pages, and the JDK's own wording.
    private static final List<String> UNREACHABLE = List.of(
            "ERR_NAME_NOT_RESOLVED", "ERR_CONNECTION_REFUSED", "ERR_CONNECTION_TIMED_OUT",
            "ERR_INTERNET_DISCONNECTED", "ERR_ADDRESS_UNREACHABLE", "ERR_CONNECTION_RESET",
            "dnsNotFound", "connectionFailure", "netTimeout", "UnknownHost");

    private BaseUrlNavigation() {}

    /** Returns {@code baseUrl}, or throws naming the missing config key. */
    public static String require(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(MISSING_BASE_URL);
        }
        return baseUrl;
    }

    /** {@code driver.get(url)}, with a clearer error when {@code url} cannot be reached. */
    public static void go(WebDriver driver, String url) {
        try {
            driver.get(url);
        } catch (WebDriverException e) {
            String msg = String.valueOf(e.getMessage());
            for (String marker : UNREACHABLE) {
                if (msg.contains(marker)) {
                    throw new WebDriverException(
                            "Could not reach " + url + " (" + marker + ") — check execution.baseUrl "
                            + "in selenium-boot.yml, and that the site is up and reachable from this machine.", e);
                }
            }
            throw e;
        }
    }
}
