package com.seleniumboot.locator;

/**
 * Thrown when a {@link Locator} chain cannot resolve to a matching element.
 */
public class LocatorException extends RuntimeException {

    public LocatorException(String message) {
        super(message);
    }

    public LocatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
