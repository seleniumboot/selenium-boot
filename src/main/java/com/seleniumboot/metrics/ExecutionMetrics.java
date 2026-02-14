package com.seleniumboot.metrics;

import java.util.Comparator;
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

    // -----------------------------------------
    // Test Start
    // -----------------------------------------

    public static void markStart(String testId) {
        START_TIMES.put(testId, System.currentTimeMillis());
    }

    // -----------------------------------------
    // Driver Startup Time
    // -----------------------------------------

    public static void recordDriverStartup(String testId, long duration) {

        TestTiming timing = TIMINGS.computeIfAbsent(
                testId,
                id -> new TestTiming(id, Thread.currentThread().getName())
        );

        timing.setDriverStartupTime(duration);
    }

    // -----------------------------------------
    // Test End
    // -----------------------------------------

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

    // -----------------------------------------
    // Summary
    // -----------------------------------------

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

    // -----------------------------------------
    // Optional: Clear metrics between suites
    // -----------------------------------------

    public static void reset() {
        START_TIMES.clear();
        TIMINGS.clear();
        TOTAL_DURATION.set(0);
    }
}
