package com.seleniumboot.ci;

/**
 * Thrown when a build quality gate defined in the {@code ci:} config block is breached.
 * Propagating this as a runtime exception causes the Maven/Gradle build to fail.
 */
public class BuildQualityGateException extends RuntimeException {

    public BuildQualityGateException(String message) {
        super(message);
    }
}
