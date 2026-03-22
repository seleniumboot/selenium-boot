package com.seleniumboot.assertion;

import com.seleniumboot.api.SeleniumBootApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects soft assertion failures without throwing immediately.
 * Obtain via {@code softAssert()} in {@link com.seleniumboot.test.BaseTest} or
 * {@link com.seleniumboot.test.BasePage}. The framework flushes all failures at
 * test end — if any exist the test is marked as FAILED with all messages combined.
 *
 * <pre>
 * softAssert().that(title.equals("Dashboard"), "Title should be Dashboard, was: " + title);
 * softAssert().that(menuVisible, "Navigation menu should be visible");
 * // test continues even if assertions fail — framework reports all at the end
 * </pre>
 */
@SeleniumBootApi(since = "1.0.0")
public final class SoftAssertionCollector {

    private final List<String> failures = new ArrayList<>();

    SoftAssertionCollector() {}  // package-private — obtained via SoftAssertions.get()

    /**
     * Checks {@code condition}. If false, records {@code message} as a failure.
     * Does NOT throw — test execution continues.
     *
     * @param condition the assertion to check
     * @param message   failure description shown in the report when condition is false
     * @return this collector (for chaining)
     */
    public SoftAssertionCollector that(boolean condition, String message) {
        if (!condition) {
            failures.add(message);
        }
        return this;
    }

    /** Returns {@code true} if at least one {@code that()} call evaluated to {@code false}. */
    public boolean hasFailed() {
        return !failures.isEmpty();
    }

    /** Returns an unmodifiable snapshot of all collected failure messages. */
    public List<String> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /** Clears all collected failures. Called by the framework after flushing. */
    public void clear() {
        failures.clear();
    }
}
