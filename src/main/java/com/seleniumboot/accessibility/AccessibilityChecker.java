package com.seleniumboot.accessibility;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runs axe-core accessibility scans against the active browser page.
 *
 * <p>axe-core is bundled in the JAR — no internet connection or extra dependency required.
 * The library is injected once per page load; subsequent calls on the same page reuse the
 * already-injected instance.
 *
 * <p>Prefer the fluent API via {@link AccessibilityAssert} (accessed through
 * {@code accessibility()} in {@code BaseTest}) over calling this class directly.
 */
public final class AccessibilityChecker {

    private static final String AXE_RESOURCE = "/com/seleniumboot/accessibility/axe.min.js";

    private static volatile String axeSource;

    private AccessibilityChecker() {}

    /**
     * Runs an axe-core scan on the full page and returns the result.
     *
     * @throws IllegalStateException if no active WebDriver session exists
     */
    public static AccessibilityResult scan() {
        return scan(null, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Runs an axe-core scan scoped to {@code context} (a CSS selector or {@code null} for full page).
     */
    public static AccessibilityResult scan(String context) {
        return scan(context, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Full scan with a CSS context scope, selectors to exclude, and WCAG tag filters.
     *
     * @param context         CSS selector to restrict the scan (null = full page)
     * @param excludeSelectors CSS selectors whose subtrees axe-core should skip
     * @param tags            axe-core rule tags to run, e.g. {@code ["wcag2a", "wcag2aa"]};
     *                        empty list runs all rules
     */
    public static AccessibilityResult scan(String context,
                                           List<String> excludeSelectors,
                                           List<String> tags) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException(
                "[Accessibility] No active WebDriver. Call accessibility() after open()."
            );
        }
        if (!(driver instanceof JavascriptExecutor)) {
            throw new UnsupportedOperationException(
                "[Accessibility] Browser does not support JavaScript execution."
            );
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;

        injectAxeIfNeeded(js);

        String runScript = buildRunScript(context, excludeSelectors, tags);

        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) js.executeAsyncScript(runScript);

        String url = driver.getCurrentUrl();
        return parse(url, raw);
    }

    // ── axe-core injection ───────────────────────────────────────────────────

    private static void injectAxeIfNeeded(JavascriptExecutor js) {
        Boolean alreadyLoaded = (Boolean) js.executeScript(
            "return (typeof axe !== 'undefined' && typeof axe.run === 'function');"
        );
        if (Boolean.TRUE.equals(alreadyLoaded)) return;

        js.executeScript(loadAxeSource());
    }

    private static String loadAxeSource() {
        if (axeSource != null) return axeSource;
        synchronized (AccessibilityChecker.class) {
            if (axeSource != null) return axeSource;
            try (InputStream in = AccessibilityChecker.class.getResourceAsStream(AXE_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException(
                        "[Accessibility] axe.min.js not found in classpath at " + AXE_RESOURCE
                    );
                }
                axeSource = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("[Accessibility] Failed to load axe.min.js", e);
            }
        }
        return axeSource;
    }

    // ── script builder ───────────────────────────────────────────────────────

    /**
     * Builds the {@code executeAsyncScript} payload.
     * axe.run() is async — we use the Selenium async callback pattern.
     */
    private static String buildRunScript(String context,
                                         List<String> excludeSelectors,
                                         List<String> tags) {
        StringBuilder sb = new StringBuilder();
        sb.append("var callback = arguments[arguments.length - 1];");

        // Build axe context object
        if (context != null || !excludeSelectors.isEmpty()) {
            sb.append("var ctx = {};");
            if (context != null) {
                sb.append("ctx.include = [").append(jsString(context)).append("];");
            }
            if (!excludeSelectors.isEmpty()) {
                sb.append("ctx.exclude = [");
                for (int i = 0; i < excludeSelectors.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("[").append(jsString(excludeSelectors.get(i))).append("]");
                }
                sb.append("];");
            }
        } else {
            sb.append("var ctx = document;");
        }

        // Build axe options object
        sb.append("var opts = {};");
        if (!tags.isEmpty()) {
            sb.append("opts.runOnly = { type: 'tag', values: [");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(jsString(tags.get(i)));
            }
            sb.append("] };");
        }

        sb.append("axe.run(ctx, opts, function(err, results) {");
        sb.append("  if (err) { callback({ error: err.message }); return; }");
        sb.append("  callback(results);");
        sb.append("});");

        return sb.toString();
    }

    private static String jsString(String value) {
        return "'" + value.replace("'", "\\'") + "'";
    }

    // ── result parser ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static AccessibilityResult parse(String url, Map<String, Object> raw) {
        if (raw == null) {
            return new AccessibilityResult(url, Collections.emptyList(), 0, 0);
        }

        List<AccessibilityViolation> violations = new ArrayList<>();
        Object violationsObj = raw.get("violations");
        if (violationsObj instanceof List) {
            for (Object v : (List<?>) violationsObj) {
                if (v instanceof Map) {
                    violations.add(parseViolation((Map<String, Object>) v));
                }
            }
        }

        int passes    = sizeOf(raw.get("passes"));
        int incomplete = sizeOf(raw.get("incomplete"));

        return new AccessibilityResult(url, violations, passes, incomplete);
    }

    @SuppressWarnings("unchecked")
    private static AccessibilityViolation parseViolation(Map<String, Object> v) {
        String id          = str(v.get("id"));
        String description = str(v.get("description"));
        String help        = str(v.get("help"));
        String helpUrl     = str(v.get("helpUrl"));
        Impact impact      = Impact.fromString(str(v.get("impact")));

        List<AccessibilityViolation.NodeDetail> nodes = new ArrayList<>();
        Object nodesObj = v.get("nodes");
        if (nodesObj instanceof List) {
            for (Object n : (List<?>) nodesObj) {
                if (n instanceof Map) {
                    nodes.add(parseNode((Map<String, Object>) n));
                }
            }
        }

        return new AccessibilityViolation(id, description, help, helpUrl, impact, nodes);
    }

    @SuppressWarnings("unchecked")
    private static AccessibilityViolation.NodeDetail parseNode(Map<String, Object> n) {
        String html           = str(n.get("html"));
        String failureSummary = str(n.get("failureSummary"));

        // target is a List<List<String>> in axe-core — flatten to a single CSS path string
        String target = "";
        Object targetObj = n.get("target");
        if (targetObj instanceof List) {
            List<?> targetList = (List<?>) targetObj;
            if (!targetList.isEmpty()) {
                Object first = targetList.get(0);
                if (first instanceof List) {
                    // Shadow DOM target: [[selector, shadow-selector], ...]
                    target = joinList((List<?>) first, " > ");
                } else {
                    target = joinList(targetList, " > ");
                }
            }
        }

        return new AccessibilityViolation.NodeDetail(html, target, failureSummary);
    }

    private static String joinList(List<?> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(item);
        }
        return sb.toString();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static int sizeOf(Object o) {
        if (o instanceof List) return ((List<?>) o).size();
        if (o instanceof Number) return ((Number) o).intValue();
        return 0;
    }
}
