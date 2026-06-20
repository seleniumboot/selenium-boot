package com.seleniumboot.accessibility;

import com.seleniumboot.api.SeleniumBootApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fluent accessibility assertion builder backed by axe-core.
 *
 * <p>Create an instance via {@code accessibility()} in {@code BaseTest} or
 * {@code BaseJUnit5Test}, then chain configuration methods and call {@link #run()}
 * to execute the scan and assert no violations:
 *
 * <pre>
 * // Assert zero violations at SERIOUS or above (WCAG 2.1 AA)
 * accessibility()
 *     .withLevel(Impact.SERIOUS)
 *     .withTags("wcag2a", "wcag2aa")
 *     .excluding("#cookie-banner", "#chat-widget")
 *     .run();
 *
 * // Scope scan to a specific page section
 * accessibility()
 *     .withContext("#main-content")
 *     .run();
 *
 * // Collect results without asserting (for custom inspection)
 * AccessibilityResult result = accessibility().collect();
 * Assert.assertEquals(result.violationCount(), 0, result.toString());
 * </pre>
 *
 * <p>axe-core is bundled in the JAR — no internet connection or extra dependency needed.
 * The library is injected into the browser once and reused for subsequent scans on the same page.
 */
@SeleniumBootApi(since = "2.5.0")
public final class AccessibilityAssert {

    private Impact minimumImpact      = Impact.MINOR;
    private String context            = null;
    private final List<String> excludeSelectors = new ArrayList<>();
    private final List<String> tags             = new ArrayList<>();

    private AccessibilityAssert() {}

    /** Creates a new {@code AccessibilityAssert} with default settings (all rules, all impacts). */
    public static AccessibilityAssert create() {
        return new AccessibilityAssert();
    }

    // ── configuration ────────────────────────────────────────────────────────

    /**
     * Sets the minimum {@link Impact} level that will cause a test failure.
     * Violations below this level are reported in the result but do not fail the assertion.
     *
     * <p>Example — fail only on {@code SERIOUS} or {@code CRITICAL} violations:
     * <pre>
     * accessibility().withLevel(Impact.SERIOUS).run();
     * </pre>
     *
     * @param minimum the least severe impact that should fail the test
     * @return this instance for chaining
     */
    public AccessibilityAssert withLevel(Impact minimum) {
        this.minimumImpact = minimum;
        return this;
    }

    /**
     * Restricts the scan to the subtree rooted at the given CSS selector.
     * Use to test a specific component or section of the page.
     *
     * <pre>
     * accessibility().withContext("#checkout-form").run();
     * </pre>
     *
     * @param cssSelector CSS selector for the scan root element
     * @return this instance for chaining
     */
    public AccessibilityAssert withContext(String cssSelector) {
        this.context = cssSelector;
        return this;
    }

    /**
     * Excludes one or more CSS selectors from the scan.
     * Useful for known third-party widgets with violations outside your control.
     *
     * <pre>
     * accessibility().excluding("#intercom-widget", ".third-party-banner").run();
     * </pre>
     *
     * @param cssSelectors one or more CSS selectors to exclude
     * @return this instance for chaining
     */
    public AccessibilityAssert excluding(String... cssSelectors) {
        excludeSelectors.addAll(Arrays.asList(cssSelectors));
        return this;
    }

    /**
     * Restricts which axe-core rules are run to those matching the given WCAG tags.
     * Common tags: {@code wcag2a}, {@code wcag2aa}, {@code wcag2aaa}, {@code wcag21a},
     * {@code wcag21aa}, {@code wcag22aa}, {@code best-practice}.
     *
     * <pre>
     * // Run only WCAG 2.1 AA rules
     * accessibility().withTags("wcag2a", "wcag21aa").run();
     * </pre>
     *
     * @param axeTags one or more axe-core rule tags
     * @return this instance for chaining
     */
    public AccessibilityAssert withTags(String... axeTags) {
        tags.addAll(Arrays.asList(axeTags));
        return this;
    }

    // ── terminal operations ───────────────────────────────────────────────────

    /**
     * Executes the accessibility scan and returns the raw {@link AccessibilityResult}
     * without asserting. Use this when you want to inspect violations programmatically
     * or write custom assertions.
     *
     * <pre>
     * AccessibilityResult result = accessibility().withLevel(Impact.SERIOUS).collect();
     * Assert.assertTrue(result.violationCount() &lt;= 2, "Too many violations: " + result);
     * </pre>
     */
    public AccessibilityResult collect() {
        return AccessibilityChecker.scan(context, excludeSelectors, tags);
    }

    /**
     * Executes the scan and asserts that there are no violations at or above the
     * configured {@link #withLevel(Impact) impact level}.
     *
     * <p>Throws {@link AssertionError} with a detailed report if violations are found:
     * <ul>
     *   <li>Rule ID and severity</li>
     *   <li>Plain-English fix guidance</li>
     *   <li>CSS selector path to each offending element</li>
     *   <li>Link to the axe-core rule documentation</li>
     * </ul>
     *
     * @throws AssertionError if one or more violations are found at the configured level
     */
    public void run() {
        AccessibilityResult result = collect();
        List<AccessibilityViolation> failing = result.violationsAtLevel(minimumImpact);

        if (failing.isEmpty()) return;

        String report = buildReport(result.url(), failing);
        throw new AssertionError(report);
    }

    // ── report builder ────────────────────────────────────────────────────────

    private String buildReport(String url, List<AccessibilityViolation> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[Accessibility] ")
          .append(violations.size())
          .append(" violation")
          .append(violations.size() == 1 ? "" : "s")
          .append(" found on: ")
          .append(url)
          .append("\n");

        if (!tags.isEmpty()) {
            sb.append("  Rules: ").append(String.join(", ", tags)).append("\n");
        }
        if (minimumImpact != Impact.MINOR) {
            sb.append("  Minimum impact: ").append(minimumImpact).append("\n");
        }

        for (int i = 0; i < violations.size(); i++) {
            AccessibilityViolation v = violations.get(i);
            sb.append("\n  ").append(i + 1).append(". [").append(v.impact()).append("] ")
              .append(v.id()).append("\n");
            sb.append("     ").append(v.description()).append("\n");
            sb.append("     Fix: ").append(v.help()).append("\n");
            if (!v.helpUrl().isEmpty()) {
                sb.append("     Docs: ").append(v.helpUrl()).append("\n");
            }
            List<AccessibilityViolation.NodeDetail> nodes = v.nodes();
            int shown = Math.min(nodes.size(), 3);
            for (int j = 0; j < shown; j++) {
                AccessibilityViolation.NodeDetail nd = nodes.get(j);
                sb.append("     → ").append(nd.target()).append("\n");
                if (!nd.failureSummary().isEmpty()) {
                    String summary = nd.failureSummary().replace("\n", "\n       ");
                    sb.append("       ").append(summary).append("\n");
                }
            }
            if (nodes.size() > 3) {
                sb.append("     … and ").append(nodes.size() - 3).append(" more node(s)\n");
            }
        }

        return sb.toString();
    }
}
