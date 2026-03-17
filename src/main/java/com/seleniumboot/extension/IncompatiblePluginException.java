package com.seleniumboot.extension;

/**
 * Thrown when a plugin's minimum framework version requirement is not met.
 *
 * @see FrameworkVersion#requireAtLeast(String)
 */
public class IncompatiblePluginException extends RuntimeException {

    public IncompatiblePluginException(String message) {
        super(message);
    }
}
