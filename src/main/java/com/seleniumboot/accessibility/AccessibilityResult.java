package com.seleniumboot.accessibility;

import com.seleniumboot.api.SeleniumBootApi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The full result of an axe-core accessibility scan on a page or element subtree.
 *
 * <pre>
 * AccessibilityResult result = accessibility().collect();
 * System.out.println("Violations: " + result.violationCount());
 * result.violations().forEach(System.out::println);
 * </pre>
 */
@SeleniumBootApi(since = "2.5.0")
public final class AccessibilityResult {

    private final String url;
    private final List<AccessibilityViolation> violations;
    private final int passCount;
    private final int incompleteCount;

    public AccessibilityResult(String url,
                               List<AccessibilityViolation> violations,
                               int passCount,
                               int incompleteCount) {
        this.url            = url;
        this.violations     = violations == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(violations);
        this.passCount      = passCount;
        this.incompleteCount = incompleteCount;
    }

    /** URL of the page that was scanned. */
    public String url() { return url; }

    /** All violations found during the scan. */
    public List<AccessibilityViolation> violations() { return violations; }

    /** Total number of violations. */
    public int violationCount() { return violations.size(); }

    /** Number of rules that passed. */
    public int passCount() { return passCount; }

    /** Number of rules axe-core could not determine (need manual review). */
    public int incompleteCount() { return incompleteCount; }

    /** Returns {@code true} if no violations were found. */
    public boolean isClean() { return violations.isEmpty(); }

    /**
     * Returns only the violations at or above the given impact level.
     *
     * <pre>
     * result.violationsAtLevel(Impact.SERIOUS).forEach(System.out::println);
     * </pre>
     */
    public List<AccessibilityViolation> violationsAtLevel(Impact minimum) {
        return violations.stream()
                .filter(v -> v.impact().isAtLeast(minimum))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "AccessibilityResult{url='" + url + "', violations=" + violations.size() +
               ", passed=" + passCount + ", incomplete=" + incompleteCount + "}";
    }
}
