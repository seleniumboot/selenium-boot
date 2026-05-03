package com.seleniumboot.db;

/**
 * Thrown when a database assertion fails (e.g. row not found, wrong value).
 * Extends {@link AssertionError} so test frameworks treat it as a test failure.
 */
public class DbAssertException extends AssertionError {

    public DbAssertException(String message) {
        super(message);
    }

    public DbAssertException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
