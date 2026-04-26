package com.seleniumboot.flakiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Analyses historical test run data to produce per-test flakiness risk scores.
 *
 * <p>Reads the last {@code flakiness.historyRuns} JSON files from
 * {@code target/metrics-history/}, counts pass/fail outcomes per test, and
 * classifies each test as {@code STABLE} (&lt;10% failure), {@code WATCH}
 * (10–{@code highRiskThreshold}%), or {@code HIGH} (&gt;= threshold).
 *
 * <p>Results are exported to {@code target/flakiness-report.json} and surfaced in
 * the HTML report's "Flakiness Radar" section.
 *
 * <p>Enable via:
 * <pre>
 * flakiness:
 *   historyRuns: 20
 *   highRiskThreshold: 33
 *   failOnHighFlakiness: false
 * </pre>
 */
@SeleniumBootApi(since = "1.8.0")
public final class FlakinessAnalyzer {

    private static final Logger LOG = Logger.getLogger(FlakinessAnalyzer.class.getName());

    /** Holds the result of the last analysis for this JVM run (used by report generator). */
    private static volatile List<FlakinessScore> lastResult = Collections.emptyList();

    private FlakinessAnalyzer() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public static List<FlakinessScore> getLastResult() {
        return lastResult;
    }

    /**
     * Runs the analysis and exports results.
     * Called from {@code SuiteExecutionListener.onFinish()}.
     */
    public static void analyze() {
        try {
            SeleniumBootConfig.Flakiness cfg = config();
            int    historyRuns     = cfg != null ? cfg.getHistoryRuns()       : 20;
            double highThreshold   = cfg != null ? cfg.getHighRiskThreshold() : 33.0;

            List<File> historyFiles = loadHistoryFiles(historyRuns);
            if (historyFiles.isEmpty()) return;

            // testId → [pass, fail] counts
            Map<String, int[]> counts = new LinkedHashMap<>();
            ObjectMapper mapper = new ObjectMapper();

            for (File f : historyFiles) {
                try {
                    JsonNode root = mapper.readTree(f);
                    JsonNode tests = root.path("tests");
                    for (JsonNode t : tests) {
                        String tid    = t.path("testId").asText("");
                        String status = t.path("status").asText("");
                        if (tid.isEmpty()) continue;
                        counts.computeIfAbsent(tid, k -> new int[]{0, 0});
                        if ("FAILED".equals(status))  counts.get(tid)[1]++;
                        else if ("PASSED".equals(status)) counts.get(tid)[0]++;
                    }
                } catch (IOException e) {
                    LOG.warning("[FlakinessAnalyzer] Skipping unreadable history file: " + f.getName());
                }
            }

            List<FlakinessScore> scores = new ArrayList<>();
            for (Map.Entry<String, int[]> entry : counts.entrySet()) {
                int pass  = entry.getValue()[0];
                int fail  = entry.getValue()[1];
                int total = pass + fail;
                if (total == 0) continue;
                double rate = (fail * 100.0) / total;
                FlakinessScore.Risk risk = FlakinessScore.classify(rate, highThreshold);
                scores.add(new FlakinessScore(entry.getKey(), total, fail, rate, risk));
            }

            // Sort: HIGH first, then by failureRate desc
            scores.sort(Comparator
                    .comparingInt((FlakinessScore s) -> s.getRisk() == FlakinessScore.Risk.HIGH ? 0
                            : s.getRisk() == FlakinessScore.Risk.WATCH ? 1 : 2)
                    .thenComparingDouble(s -> -s.getFailureRate()));

            lastResult = Collections.unmodifiableList(scores);
            export(scores);

            // CI gate
            if (cfg != null && cfg.isFailOnHighFlakiness()) {
                long highCount = scores.stream()
                        .filter(s -> s.getRisk() == FlakinessScore.Risk.HIGH).count();
                if (highCount > 0) {
                    throw new IllegalStateException(
                            "[Selenium Boot] " + highCount + " high-risk flaky test(s) found "
                            + "(threshold: " + highThreshold + "%). Build failed per "
                            + "flakiness.failOnHighFlakiness=true");
                }
            }
        } catch (IllegalStateException e) {
            throw e;  // re-throw CI gate failure
        } catch (Exception e) {
            LOG.warning("[FlakinessAnalyzer] Analysis failed (non-critical): " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // History file loading
    // ------------------------------------------------------------------

    static List<File> loadHistoryFiles(int maxRuns) {
        File dir = new File("target/metrics-history");
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return Collections.emptyList();
        // Sort newest first
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        int limit = Math.min(files.length, maxRuns);
        return Arrays.asList(files).subList(0, limit);
    }

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    private static void export(List<FlakinessScore> scores) {
        if (scores.isEmpty()) return;
        try {
            File out = new File("target/flakiness-report.json");
            List<Map<String, Object>> entries = scores.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("testId",       s.getTestId());
                m.put("runsAnalysed", s.getRunsAnalysed());
                m.put("failCount",    s.getFailCount());
                m.put("failureRate",  Math.round(s.getFailureRate() * 10.0) / 10.0);
                m.put("risk",         s.getRisk().name());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("analysedTests", scores.size());
            root.put("highRisk",  scores.stream().filter(s -> s.getRisk() == FlakinessScore.Risk.HIGH).count());
            root.put("watch",     scores.stream().filter(s -> s.getRisk() == FlakinessScore.Risk.WATCH).count());
            root.put("stable",    scores.stream().filter(s -> s.getRisk() == FlakinessScore.Risk.STABLE).count());
            root.put("scores",    entries);

            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, root);
            System.out.println("[Selenium Boot] Flakiness report → " + out.getPath()
                    + " (" + scores.stream().filter(s -> s.getRisk() == FlakinessScore.Risk.HIGH).count()
                    + " high-risk)");
        } catch (IOException e) {
            LOG.warning("[FlakinessAnalyzer] Failed to export flakiness report: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------

    private static SeleniumBootConfig.Flakiness config() {
        try { return SeleniumBootContext.getConfig().getFlakiness(); } catch (Exception e) { return null; }
    }
}
