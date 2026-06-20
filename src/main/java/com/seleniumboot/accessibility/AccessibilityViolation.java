package com.seleniumboot.accessibility;

import com.seleniumboot.api.SeleniumBootApi;

import java.util.Collections;
import java.util.List;

/**
 * A single accessibility rule violation reported by axe-core.
 *
 * <p>Each violation represents a failed WCAG / best-practice rule and contains:
 * <ul>
 *   <li>The rule identifier and a plain-English description</li>
 *   <li>An {@link Impact} severity level</li>
 *   <li>One or more {@link NodeDetail} entries — the exact DOM nodes that failed</li>
 * </ul>
 *
 * <pre>
 * for (AccessibilityViolation v : result.violations()) {
 *     System.out.println(v.id() + " [" + v.impact() + "] — " + v.description());
 *     v.nodes().forEach(n -&gt; System.out.println("  " + n.target()));
 * }
 * </pre>
 */
@SeleniumBootApi(since = "2.5.0")
public final class AccessibilityViolation {

    private final String id;
    private final String description;
    private final String help;
    private final String helpUrl;
    private final Impact impact;
    private final List<NodeDetail> nodes;

    public AccessibilityViolation(String id, String description, String help,
                                  String helpUrl, Impact impact, List<NodeDetail> nodes) {
        this.id          = id;
        this.description = description;
        this.help        = help;
        this.helpUrl     = helpUrl;
        this.impact      = impact;
        this.nodes       = nodes == null ? Collections.emptyList() : Collections.unmodifiableList(nodes);
    }

    /** axe-core rule identifier, e.g. {@code "color-contrast"}, {@code "image-alt"}. */
    public String id() { return id; }

    /** Short description of what the rule checks. */
    public String description() { return description; }

    /** Human-readable explanation of how to fix the violation. */
    public String help() { return help; }

    /** URL to the axe-core rule documentation. */
    public String helpUrl() { return helpUrl; }

    /** Severity of this violation. */
    public Impact impact() { return impact; }

    /** DOM nodes that triggered this violation. */
    public List<NodeDetail> nodes() { return nodes; }

    @Override
    public String toString() {
        return "[" + impact + "] " + id + ": " + description +
               " (" + nodes.size() + " node" + (nodes.size() == 1 ? "" : "s") + ")";
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Details about a specific DOM node that failed an accessibility rule.
     */
    @SeleniumBootApi(since = "2.5.0")
    public static final class NodeDetail {

        private final String html;
        private final String target;
        private final String failureSummary;

        public NodeDetail(String html, String target, String failureSummary) {
            this.html           = html;
            this.target         = target;
            this.failureSummary = failureSummary;
        }

        /** Outer HTML of the offending element. */
        public String html() { return html; }

        /** CSS selector path to the element (axe-core {@code target} array joined with {@code " > "}). */
        public String target() { return target; }

        /** Axe-core failure summary with fix guidance for this node. */
        public String failureSummary() { return failureSummary; }

        @Override
        public String toString() {
            return target + "  →  " + failureSummary;
        }
    }
}
