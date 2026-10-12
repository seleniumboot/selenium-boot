package com.seleniumboot.failure;

/** Why a test failed, as decided by {@link FailureClassifier}. */
public enum FailureCategory {
    LOCATOR_NOT_FOUND("Locator not found"),
    ELEMENT_NOT_INTERACTABLE("Element not interactable"),
    STALE_ELEMENT("Stale element"),
    TIMEOUT("Timeout"),
    ASSERTION_MISMATCH("Assertion mismatch"),
    NAVIGATION_ERROR("Navigation error"),
    BROWSER_CRASH("Browser crash"),
    UNKNOWN("Unknown");

    private final String label;

    FailureCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
