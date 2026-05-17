package com.seleniumboot.quarantine;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads {@code selenium-quarantine.yml} and exposes lookups for whether a given test
 * or Cucumber scenario is quarantined.
 *
 * <h3>File resolution order</h3>
 * <ol>
 *   <li>System property {@code -Dselenium.boot.quarantine=/path/to/file.yml}</li>
 *   <li>Working directory — {@code ./selenium-quarantine.yml}</li>
 *   <li>Classpath — {@code selenium-quarantine.yml}</li>
 * </ol>
 *
 * <h3>Supported entry types</h3>
 * <pre>{@code
 * quarantine:
 *
 *   # ── TestNG / JUnit 5 ───────────────────────────────────────────────────
 *   # Specific method
 *   - com.example.tests.LoginTest#loginWithExpiredSession
 *   # Entire class (all methods)
 *   - com.example.tests.PaymentTest
 *   # With reason
 *   - test: com.example.tests.SearchTest#searchSpecial
 *     reason: "JIRA-1234 — Unicode broken"
 *
 *   # ── Cucumber: by tag (all scenarios that carry the tag) ────────────────
 *   - "@smoke"
 *   - test: "@regression"
 *     reason: "Regression suite unstable after payment refactor"
 *
 *   # ── Cucumber: by feature file (all scenarios in the file) ──────────────
 *   - login.feature
 *   - test: features/payment.feature
 *     reason: "Payment service down — JIRA-456"
 *
 *   # ── Cucumber: specific scenario (feature file + scenario name) ──────────
 *   - "checkout.feature#Checkout with 3D Secure"
 *   - test: "login.feature#Login with expired token"
 *     reason: "Token refresh flaky on CI"
 * }</pre>
 *
 * <p>Cucumber tests can also be quarantined by adding the configured tag
 * (default {@code @quarantine}) directly to the scenario in the {@code .feature} file —
 * in that case no YAML entry is needed.
 *
 * <p>The file is read once per JVM invocation and cached. Call {@link #reload()} to force
 * a fresh read (useful in tests).
 */
public final class QuarantineLoader {

    private static volatile Set<String>         IDS     = null;
    private static volatile Map<String, String> REASONS = null;

    private QuarantineLoader() {}

    // ── Java (TestNG / JUnit 5) ───────────────────────────────────────────────

    /**
     * Returns {@code true} if the Java test identified by {@code testId} is quarantined.
     *
     * <p>{@code testId} format: {@code fully.qualified.ClassName#methodName}.
     * A class-only entry ({@code fully.qualified.ClassName}) matches every method in that class.
     */
    public static boolean isQuarantined(String testId) {
        ensureLoaded();
        if (IDS.contains(testId)) return true;
        // class-only match
        int hash = testId.indexOf('#');
        return hash > 0 && IDS.contains(testId.substring(0, hash));
    }

    /**
     * Returns the reason string for a quarantined Java test, or a generic message if no
     * reason was provided.
     */
    public static String getReason(String testId) {
        ensureLoaded();
        String r = REASONS.get(testId);
        if (r == null) {
            int hash = testId.indexOf('#');
            r = hash > 0 ? REASONS.get(testId.substring(0, hash)) : null;
        }
        return r != null ? r : "listed in selenium-quarantine.yml";
    }

    // ── Cucumber ─────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the Cucumber scenario should be quarantined based on
     * entries in the YAML file.
     *
     * <p>Three matching strategies are applied in order:
     * <ol>
     *   <li>Tag entry ({@code "@smoke"}) — matches if the scenario carries that Cucumber tag.</li>
     *   <li>Feature file entry ({@code "login.feature"}) — matches if the scenario's URI
     *       ends with that file path.</li>
     *   <li>Feature + name entry ({@code "login.feature#Login with expired session"}) — matches
     *       if both the file path and scenario name match.</li>
     * </ol>
     *
     * <p>This method accepts plain string parameters so that {@code QuarantineLoader} has no
     * compile-time dependency on the Cucumber API.
     *
     * @param scenarioTags   result of {@code Scenario.getSourceTagNames()} — e.g. {@code ["@smoke", "@login"]}
     * @param featureUri     result of {@code Scenario.getUri().toString()} — e.g.
     *                       {@code "classpath:features/login.feature"}
     * @param scenarioName   result of {@code Scenario.getName()} — e.g. {@code "Login with valid credentials"}
     */
    public static boolean isQuarantinedScenario(
            Collection<String> scenarioTags, String featureUri, String scenarioName) {
        ensureLoaded();
        for (String entry : IDS) {
            if (matchesScenario(entry, scenarioTags, featureUri, scenarioName)) return true;
        }
        return false;
    }

    /**
     * Returns the reason for the first YAML entry that matches this Cucumber scenario,
     * or a generic message if the entry had no reason.
     */
    public static String getScenarioReason(
            Collection<String> scenarioTags, String featureUri, String scenarioName) {
        ensureLoaded();
        for (String entry : IDS) {
            if (matchesScenario(entry, scenarioTags, featureUri, scenarioName)) {
                return REASONS.getOrDefault(entry, "listed in selenium-quarantine.yml");
            }
        }
        return "listed in selenium-quarantine.yml";
    }

    // ── Generic helpers ───────────────────────────────────────────────────────

    /** Returns the number of quarantined entries currently loaded. */
    public static int size() {
        ensureLoaded();
        return IDS.size();
    }

    /**
     * Clears the cached data so the file is re-read on the next call.
     * Intended for use in tests only.
     */
    public static synchronized void reload() {
        IDS     = null;
        REASONS = null;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Checks whether a single YAML entry matches the given Cucumber scenario data.
     * Entry types:
     * <ul>
     *   <li>{@code "@tag"}           — Cucumber tag match</li>
     *   <li>{@code "x.feature"}      — feature file name match</li>
     *   <li>{@code "x.feature#name"} — feature file + scenario name match</li>
     *   <li>anything else            — Java class/method entry, not a Cucumber entry</li>
     * </ul>
     */
    public static boolean matchesScenario(String entry,
            Collection<String> tags, String featureUri, String scenarioName) {

        // 1. Tag entry: "@smoke"
        if (entry.startsWith("@")) {
            String entryTag = entry.substring(1);
            for (String t : tags) {
                String normalized = t.startsWith("@") ? t.substring(1) : t;
                if (normalized.equalsIgnoreCase(entryTag)) return true;
            }
            return false;
        }

        // 2. Feature + scenario name: "login.feature#Login with expired session"
        if (entry.contains(".feature#")) {
            int hash = entry.lastIndexOf('#');
            String featurePart = entry.substring(0, hash);
            String namePart    = entry.substring(hash + 1).trim();
            return featureUriMatches(featurePart, featureUri)
                && namePart.equalsIgnoreCase(scenarioName != null ? scenarioName.trim() : "");
        }

        // 3. Feature file: "login.feature" or "features/login.feature"
        if (entry.endsWith(".feature")) {
            return featureUriMatches(entry, featureUri);
        }

        // Otherwise: Java class/method entry — not a Cucumber entry
        return false;
    }

    /**
     * Checks whether a feature file path entry matches a Cucumber scenario URI.
     *
     * <p>The URI may be prefixed by {@code classpath:}, {@code file://}, etc.
     * The entry may be just a filename ({@code login.feature}) or a relative path
     * ({@code features/login.feature}).
     */
    public static boolean featureUriMatches(String entry, String featureUri) {
        if (featureUri == null) return false;
        String uri   = featureUri.replace('\\', '/');
        String check = entry.replace('\\', '/');
        // ends with "/features/login.feature", "/login.feature", or equals exactly
        return uri.endsWith("/" + check) || uri.endsWith(check) || uri.equals(check);
    }

    private static void ensureLoaded() {
        if (IDS != null) return;
        synchronized (QuarantineLoader.class) {
            if (IDS != null) return;
            Set<String>         ids     = new LinkedHashSet<>();
            Map<String, String> reasons = new LinkedHashMap<>();
            InputStream         is      = findFile();
            if (is != null) parseYaml(is, ids, reasons);
            IDS     = Collections.unmodifiableSet(ids);
            REASONS = Collections.unmodifiableMap(reasons);
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseYaml(InputStream is, Set<String> ids, Map<String, String> reasons) {
        try (InputStream stream = is) {
            Object doc = new Yaml().load(stream);
            if (!(doc instanceof Map)) return;
            Object raw = ((Map<?, ?>) doc).get("quarantine");
            if (!(raw instanceof List)) return;

            for (Object item : (List<?>) raw) {
                if (item instanceof String) {
                    String id = ((String) item).trim();
                    if (!id.isEmpty()) ids.add(id);

                } else if (item instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) item;
                    Object test = entry.get("test");
                    if (test instanceof String) {
                        String id = ((String) test).trim();
                        if (!id.isEmpty()) {
                            ids.add(id);
                            Object reason = entry.get("reason");
                            if (reason instanceof String) reasons.put(id, (String) reason);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Quarantine] Could not parse selenium-quarantine.yml: " + e.getMessage());
        }
    }

    private static InputStream findFile() {
        String prop = System.getProperty("selenium.boot.quarantine");
        if (prop != null) {
            try { return new FileInputStream(prop); }
            catch (Exception ignored) {
                System.err.println("[Quarantine] File not found at -Dselenium.boot.quarantine=" + prop);
            }
        }
        File local = new File("selenium-quarantine.yml");
        if (local.exists()) {
            try { return new FileInputStream(local); } catch (Exception ignored) {}
        }
        InputStream cp = QuarantineLoader.class.getClassLoader()
                .getResourceAsStream("selenium-quarantine.yml");
        return cp;
    }
}
