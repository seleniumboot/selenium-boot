package com.seleniumboot.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionMetrics {
    private static final ConcurrentHashMap<String, Long> START_TIMES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> DURATIONS = new ConcurrentHashMap<>();
    private static final AtomicLong TOTAL_DURATION = new AtomicLong(0);

    private ExecutionMetrics() {}

    public static void markStart(String testId) {
        START_TIMES.put(testId, System.currentTimeMillis());
    }

    public static void markEnd(String testId) {
        Long start = START_TIMES.remove(testId);

        if (start == null) {
            return;
        }

        Long duration = System.currentTimeMillis() - start;
        DURATIONS.put(testId, duration);
        TOTAL_DURATION.addAndGet(duration);
    }
    public static void printSummary() {
        int totalTests = DURATIONS.size();
        long totalTime = TOTAL_DURATION.get();

        long slowest = 0;
        String slowestTest = null;

        for (var entry : DURATIONS.entrySet()) {
            if (entry.getValue() > slowest) {
                slowest = entry.getValue();
                slowestTest = entry.getKey();
            }
        }

        System.out.println("\n===== Selenium Boot Execution Metrics =====");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Total Time: " + totalTime + " ms");

        if (totalTests > 0) {
            System.out.println("Slowest Test: "
                    + slowestTest + " (" + slowest + " ms)");
        }
        System.out.println("===========================================\n");
    }
}
