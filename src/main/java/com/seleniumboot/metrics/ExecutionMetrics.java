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

    /** Returns the wall-clock start time for the test, or 0 if not found. */
    public static long getTestStartTime(String testId) {
        Long t = START_TIMES.get(testId);
        return t != null ? t : 0L;
    }

    // ==========================================================
    // Step Logging
    // ==========================================================

    public static void recordStep(String testId, com.seleniumboot.steps.StepRecord step) {
        TIMINGS.computeIfAbsent(testId,
                id -> new TestTiming(id, Thread.currentThread().getName()))
               .addStep(step);
    }

    public static void clearSteps(String testId) {
        TestTiming t = TIMINGS.get(testId);
        if (t != null) t.clearSteps();
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
    // Screenshot
    // ==========================================================

    public static void recordScreenshot(String testId, String path) {
        if (path == null) return;
        TestTiming timing = TIMINGS.computeIfAbsent(
                testId,
                id -> new TestTiming(id, Thread.currentThread().getName())
        );
        timing.setScreenshotPath(path);
    }

    // ==========================================================
    // Error Recording
    // ==========================================================

    public static void recordError(String testId, Throwable t) {
        TIMINGS.computeIfPresent(testId, (k, v) -> {
            v.setErrorMessage(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            v.setStackTrace(buildStackTrace(t));
            return v;
        });
    }

    public static void recordDescription(String testId, String description) {
        if (description == null || description.isEmpty()) return;
        TIMINGS.computeIfAbsent(testId,
                id -> new TestTiming(id, Thread.currentThread().getName()))
               .setDescription(description);
    }

    public static void recordTestClass(String testId, String className) {
        TIMINGS.computeIfAbsent(testId,
                id -> new TestTiming(id, Thread.currentThread().getName()))
               .setTestClassName(className);
    }

    private static String buildStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // ==========================================================
    // Retry Tracking
    // ==========================================================

    public static void recordRetry(String testId) {
        TIMINGS.computeIfAbsent(testId,
                id -> new TestTiming(id, Thread.currentThread().getName()))
               .incrementRetryCount();
    }

    // ==========================================================
    // Browser Tracking (matrix runs)
    // ==========================================================

    public static void recordBrowser(String testId, String browser) {
        TIMINGS.computeIfAbsent(testId,
                id -> new TestTiming(id, Thread.currentThread().getName()))
               .setBrowser(browser);
    }

    public static String getBrowser(String testId) {
        TestTiming t = TIMINGS.get(testId);
        return t != null ? t.getBrowser() : null;
    }

    // ==========================================================
    // Timings Snapshot (for reporters)
    // ==========================================================

    public static java.util.Collection<TestTiming> getTimings() {
        return java.util.Collections.unmodifiableCollection(TIMINGS.values());
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

        long passed   = TIMINGS.values().stream().filter(t -> "PASSED".equals(t.getStatus())).count();
        long failed   = TIMINGS.values().stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long skipped  = TIMINGS.values().stream().filter(t -> "SKIPPED".equals(t.getStatus())).count();
        long flaky    = TIMINGS.values().stream().filter(t -> t.getRetryCount() > 0).count();
        long recovered = TIMINGS.values().stream()
                .filter(t -> t.getRetryCount() > 0 && "PASSED".equals(t.getStatus())).count();
        double passRate = totalTests == 0 ? 0.0
                : Math.round((passed * 1000.0) / totalTests) / 10.0;

        report.put("totalTests", totalTests);
        report.put("passedTests", passed);
        report.put("failedTests", failed);
        report.put("skippedTests", skipped);
        report.put("passRate", passRate);
        report.put("flakyTests", flaky);
        report.put("recoveredTests", recovered);
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

            testEntry.put("testId",        timing.getTestId());
            testEntry.put("testClassName", timing.getTestClassName() != null ? timing.getTestClassName() : "");
            testEntry.put("thread",        timing.getThreadName());
            testEntry.put("status",        timing.getStatus() != null ? timing.getStatus() : "UNKNOWN");
            testEntry.put("retryCount",    timing.getRetryCount());
            testEntry.put("driverStartupMs", timing.getDriverStartupTime());
            testEntry.put("testLogicMs",   timing.getTestExecutionTime());
            testEntry.put("totalMs",       timing.getTotalTime());
            if (timing.getScreenshotPath() != null) {
                testEntry.put("screenshotPath", timing.getScreenshotPath());
            }
            if (timing.getDescription() != null) {
                testEntry.put("description", timing.getDescription());
            }
            if (timing.getErrorMessage() != null) {
                testEntry.put("errorMessage", timing.getErrorMessage());
            }
            if (timing.getStackTrace() != null) {
                testEntry.put("stackTrace", timing.getStackTrace());
            }
            if (timing.getBrowser() != null) {
                testEntry.put("browser", timing.getBrowser());
            }
            if (!timing.getSteps().isEmpty()) {
                List<Map<String, Object>> stepList = new ArrayList<>();
                for (com.seleniumboot.steps.StepRecord s : timing.getSteps()) {
                    Map<String, Object> stepEntry = new LinkedHashMap<>();
                    stepEntry.put("name",     s.getName());
                    stepEntry.put("offsetMs", s.getOffsetMs());
                    stepEntry.put("status",   s.getStatus());
                    if (s.getScreenshotBase64() != null) {
                        stepEntry.put("screenshotBase64", s.getScreenshotBase64());
                    }
                    stepList.add(stepEntry);
                }
                testEntry.put("steps", stepList);
            }

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

            // Primary output — always overwritten; used by HtmlReportGenerator
            File primary = new File("target/selenium-boot-metrics.json");
            mapper.writeValue(primary, report);

            // Timestamped copy for historical retention across CI runs
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File historyDir = new File("target/metrics-history");
            if (!historyDir.exists()) {
                historyDir.mkdirs();
            }
            mapper.writeValue(
                    new File(historyDir, "selenium-boot-metrics-" + timestamp + ".json"),
                    report
            );

            System.out.println("[Selenium Boot] Metrics exported → " + primary.getPath());
            System.out.println("[Selenium Boot] History copy     → target/metrics-history/selenium-boot-metrics-" + timestamp + ".json");

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

        index = Math.max(0, Math.min(index - 1, values.size() - 1));

        return values.get(index);
    }
}
