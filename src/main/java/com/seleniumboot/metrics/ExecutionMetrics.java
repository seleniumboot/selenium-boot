package com.seleniumboot.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ExecutionMetrics {

    private static final ConcurrentHashMap<String, Long> START_TIMES =
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, TestTiming> TIMINGS =
            new ConcurrentHashMap<>();

    private static final AtomicLong TOTAL_DURATION =
            new AtomicLong(0);

    private ExecutionMetrics() {}

    // ==========================================================
    // Test Start
    // ==========================================================

    public static void markStart(String testId) {
        START_TIMES.put(testId, System.currentTimeMillis());
    }

    // ==========================================================
    // Driver Startup
    // ==========================================================

    public static void recordDriverStartup(String testId, long duration) {

        TestTiming timing = TIMINGS.computeIfAbsent(
                testId,
                id -> new TestTiming(id, Thread.currentThread().getName())
        );

        timing.setDriverStartupTime(duration);
    }

    // ==========================================================
    // Test Status
    // ==========================================================

    public static void recordStatus(String testId, String status) {
        TestTiming timing = TIMINGS.computeIfAbsent(
                testId,
                id -> new TestTiming(id, Thread.currentThread().getName())
        );
        timing.setStatus(status);
    }

    // ==========================================================
    // Test End
    // ==========================================================

    public static void markEnd(String testId) {

        Long start = START_TIMES.remove(testId);
        if (start == null) return;

        long total = System.currentTimeMillis() - start;

        TestTiming timing = TIMINGS.computeIfAbsent(
                testId,
                id -> new TestTiming(id, Thread.currentThread().getName())
        );

        timing.setTotalTime(total);

        long logicTime = total - timing.getDriverStartupTime();
        timing.setTestExecutionTime(Math.max(logicTime, 0));

        TOTAL_DURATION.addAndGet(total);
    }

    // ==========================================================
    // Console Summary
    // ==========================================================

    public static void printSummary() {

        int totalTests = TIMINGS.size();
        long totalTime = TOTAL_DURATION.get();

        long passed  = TIMINGS.values().stream().filter(t -> "PASSED".equals(t.getStatus())).count();
        long failed  = TIMINGS.values().stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long skipped = TIMINGS.values().stream().filter(t -> "SKIPPED".equals(t.getStatus())).count();

        System.out.println("\n===== Selenium Boot Execution Metrics =====");
        System.out.println("Total Tests : " + totalTests);
        System.out.println("Passed      : " + passed);
        System.out.println("Failed      : " + failed);
        System.out.println("Skipped     : " + skipped);
        System.out.println("Total Time  : " + totalTime + " ms");

        if (totalTests > 0) {

            long average = totalTime / totalTests;

            TestTiming slowest = TIMINGS.values()
                    .stream()
                    .max(Comparator.comparingLong(TestTiming::getTotalTime))
                    .orElse(null);

            System.out.println("Average Time: " + average + " ms");

            if (slowest != null) {
                System.out.println("Slowest Test: "
                        + slowest.getTestId() + " (" + slowest.getTotalTime() + " ms)");
            }
        }

        System.out.println("===========================================\n");
    }

    // ==========================================================
    // JSON Export
    // ==========================================================

    public static void exportToJson() {

        Map<String, Object> report = new LinkedHashMap<>();

        int totalTests = TIMINGS.size();
        long totalTime = TOTAL_DURATION.get();

        List<Long> totalDurations = new ArrayList<>();
        List<Long> driverDurations = new ArrayList<>();

        for (TestTiming timing : TIMINGS.values()) {
            totalDurations.add(timing.getTotalTime());
            driverDurations.add(timing.getDriverStartupTime());
        }

        long passed  = TIMINGS.values().stream().filter(t -> "PASSED".equals(t.getStatus())).count();
        long failed  = TIMINGS.values().stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long skipped = TIMINGS.values().stream().filter(t -> "SKIPPED".equals(t.getStatus())).count();

        report.put("totalTests", totalTests);
        report.put("passedTests", passed);
        report.put("failedTests", failed);
        report.put("skippedTests", skipped);
        report.put("totalTimeMs", totalTime);
        report.put("averageTimeMs",
                totalTests == 0 ? 0 : totalTime / totalTests);

        if (!totalDurations.isEmpty()) {

            Map<String, Object> percentiles = new LinkedHashMap<>();

            percentiles.put("p50", percentile(totalDurations, 50));
            percentiles.put("p90", percentile(totalDurations, 90));
            percentiles.put("p95", percentile(totalDurations, 95));
            percentiles.put("p99", percentile(totalDurations, 99));

            report.put("executionPercentilesMs", percentiles);

            Map<String, Object> driverPercentiles = new LinkedHashMap<>();

            driverPercentiles.put("p50", percentile(driverDurations, 50));
            driverPercentiles.put("p90", percentile(driverDurations, 90));
            driverPercentiles.put("p95", percentile(driverDurations, 95));
            driverPercentiles.put("p99", percentile(driverDurations, 99));

            report.put("driverStartupPercentilesMs", driverPercentiles);
        }

        List<Map<String, Object>> testList = new ArrayList<>();

        for (TestTiming timing : TIMINGS.values()) {

            Map<String, Object> testEntry =
                    new LinkedHashMap<>();

            testEntry.put("testId", timing.getTestId());
            testEntry.put("thread", timing.getThreadName());
            testEntry.put("status",
                    timing.getStatus() != null ? timing.getStatus() : "UNKNOWN");
            testEntry.put("driverStartupMs",
                    timing.getDriverStartupTime());
            testEntry.put("testLogicMs",
                    timing.getTestExecutionTime());
            testEntry.put("totalMs",
                    timing.getTotalTime());

            testList.add(testEntry);
        }

        report.put("tests", testList);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {

            File outputDir = new File("target");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            mapper.writeValue(
                    new File("target/selenium-boot-metrics.json"),
                    report
            );

            System.out.println(
                    "[Selenium Boot] Metrics exported with percentiles."
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to export metrics JSON", e
            );
        }
    }

    // ==========================================================
    // Reset
    // ==========================================================

    public static void reset() {
        START_TIMES.clear();
        TIMINGS.clear();
        TOTAL_DURATION.set(0);
    }

    // ==========================================================
    // Percentile Calculation
    // ==========================================================

    public static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        Collections.sort(values);

        int index = (int) Math.ceil(percentile / 100.0 * values.size());

        index = Math.min(index - 1, values.size() - 1);

        return values.get(index);
    }
}
