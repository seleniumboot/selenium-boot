package com.seleniumboot.accessibility;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * Severity level of an accessibility violation, as reported by axe-core.
 *
 * <p>Ordered from most to least severe: {@code CRITICAL > SERIOUS > MODERATE > MINOR}.
 * Use with {@link AccessibilityAssert#withLevel(Impact)} to filter which violations fail the test.
 *
 * <h3>WCAG mapping</h3>
 * <ul>
 *   <li>{@link #CRITICAL} — violations that make content inaccessible to assistive tech users</li>
 *   <li>{@link #SERIOUS}  — barriers that are severe but may have workarounds</li>
 *   <li>{@link #MODERATE} — accessibility degradation without a complete blocker</li>
 *   <li>{@link #MINOR}    — best-practice deviations with low real-world impact</li>
 * </ul>
 */
@SeleniumBootApi(since = "2.5.0")
public enum Impact {

    CRITICAL(4),
    SERIOUS(3),
    MODERATE(2),
    MINOR(1);

    private final int level;

    Impact(int level) {
        this.level = level;
    }

    /** Returns {@code true} if this impact is at least as severe as {@code minimum}. */
    public boolean isAtLeast(Impact minimum) {
        return this.level >= minimum.level;
    }

    /**
     * Parses an axe-core impact string ({@code "critical"}, {@code "serious"},
     * {@code "moderate"}, {@code "minor"}) into an {@link Impact} enum value.
     * Returns {@link #MINOR} for unrecognised strings.
     */
    public static Impact fromString(String value) {
        if (value == null) return MINOR;
        switch (value.toLowerCase()) {
            case "critical": return CRITICAL;
            case "serious":  return SERIOUS;
            case "moderate": return MODERATE;
            default:         return MINOR;
        }
    }
}
