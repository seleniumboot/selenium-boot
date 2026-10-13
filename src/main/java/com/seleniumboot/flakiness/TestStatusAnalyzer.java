package com.seleniumboot.flakiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Derives a per-test {@link TestStatus} from this run's result (including retries) and the
 * earlier runs kept in {@code target/metrics-history/}.
 *
 * <p>It only labels tests that need a second look: a test that passed on its first attempt gets
 * no status, however flaky its history (the Flakiness Radar covers that). The pass/fail ratio,
 * the STABLE/WATCH/HIGH thresholds and the {@code failOnHighFlakiness} gate are unchanged.
 */
public final class TestStatusAnalyzer {

    /** Prior failures needed, with no passes among them, before a failure is called consistent. */
    static final int MIN_RUNS_FOR_CONSISTENT = 2;

    private TestStatusAnalyzer() {}

    /** One test's result in a single run. {@code status} is {@code PASSED}, {@code FAILED}, … */
    public record Outcome(String status, int retryCount) {}

    /**
     * Pure classification.
     *
     * @param current  this run's result
     * @param previous the test's earlier results, {@code PASSED} / {@code FAILED} only
     *                 (skipped runs are not evidence either way), in any order
     * @return the status, or {@code null} when there is nothing worth flagging or too little
     *         history to say
     */
    public static TestStatus classify(Outcome current, List<String> previous) {
        if (current == null) return null;
        boolean passed = "PASSED".equals(current.status());
        boolean failed = "FAILED".equals(current.status());
        if (passed && current.retryCount() > 0) return TestStatus.PASSED_ON_RETRY;
        if (!failed) return null;

        int prevPass = 0;
        int prevFail = 0;
        for (String s : previous == null ? List.<String>of() : previous) {
            if ("PASSED".equals(s)) prevPass++;
            else if ("FAILED".equals(s)) prevFail++;
        }
        if (prevPass > 0 && prevFail > 0) return TestStatus.INTERMITTENT;
        if (prevPass > 0) return TestStatus.NEWLY_FAILING;
        if (prevFail >= MIN_RUNS_FOR_CONSISTENT) return TestStatus.CONSISTENTLY_FAILING;
        return null;
    }

    /**
     * Classifies every test in {@code current} against the history on disk. Never throws: with
     * no readable history only {@link TestStatus#PASSED_ON_RETRY} can be decided.
     *
     * @param current       testId → this run's result
     * @param excludeFiles  history file names written by this JVM, so a run is never compared
     *                      with itself
     */
    public static Map<String, TestStatus> forRun(Map<String, Outcome> current, Set<String> excludeFiles) {
        Map<String, List<String>> previous = new HashMap<>();
        try {
            SeleniumBootConfig.Flakiness cfg = config();
            int historyRuns = cfg != null ? cfg.getHistoryRuns() : 20;
            ObjectMapper mapper = new ObjectMapper();
            for (File f : FlakinessAnalyzer.loadHistoryFiles(historyRuns)) {
                if (excludeFiles != null && excludeFiles.contains(f.getName())) continue;
                try {
                    for (JsonNode t : mapper.readTree(f).path("tests")) {
                        String id = t.path("testId").asText("");
                        if (id.isEmpty()) continue;
                        previous.computeIfAbsent(id, k -> new ArrayList<>()).add(t.path("status").asText(""));
                    }
                } catch (IOException ignored) {
                    // unreadable history file: skip, as FlakinessAnalyzer does
                }
            }
        } catch (RuntimeException ignored) {
            // history is best-effort; fall through with whatever was read
        }
        Map<String, TestStatus> result = new LinkedHashMap<>();
        for (Map.Entry<String, Outcome> e : current.entrySet()) {
            TestStatus s = classify(e.getValue(), previous.get(e.getKey()));
            if (s != null) result.put(e.getKey(), s);
        }
        return result;
    }

    private static SeleniumBootConfig.Flakiness config() {
        try { return SeleniumBootContext.getConfig().getFlakiness(); } catch (Exception e) { return null; }
    }
}
