package com.seleniumboot.extension;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * Thrown when a plugin's minimum framework version requirement is not met.
 *
 * @see FrameworkVersion#requireAtLeast(String)
 */
@SeleniumBootApi(since = "0.7.0")
public class IncompatiblePluginException extends RuntimeException {

    public IncompatiblePluginException(String message) {
        super(message);
    }
}
