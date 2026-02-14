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

        System.out.println("\n===== Selenium Boot Execution Metrics =====");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Total Time: " + totalTime + " ms");

        if (totalTests > 0) {

            long average = totalTime / totalTests;

            TestTiming slowest = TIMINGS.values()
                    .stream()
                    .max(Comparator.comparingLong(TestTiming::getTotalTime))
                    .orElse(null);

            System.out.println("Average Time: " + average + " ms");

            if (slowest != null) {
                System.out.println("Slowest Test: "
                        + slowest+ " (" + slowest.getTotalTime() + " ms)");
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

        report.put("totalTests", totalTests);
        report.put("totalTimeMs", totalTime);
        report.put("averageTimeMs",
                totalTests == 0 ? 0 : totalTime / totalTests);

        List<Map<String, Object>> testList = new ArrayList<>();

        for (TestTiming timing : TIMINGS.values()) {

            Map<String, Object> testEntry = new LinkedHashMap<>();

            testEntry.put("testId", timing.getTestId());
            testEntry.put("thread", timing.getThreadName());
            testEntry.put("driverStartupMs", timing.getDriverStartupTime());
            testEntry.put("testLogicMs", timing.getTestExecutionTime());
            testEntry.put("totalMs", timing.getTotalTime());

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
                    "[Selenium Boot] Metrics exported to target/selenium-boot-metrics.json"
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
}
