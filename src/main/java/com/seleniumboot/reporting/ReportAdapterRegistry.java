package com.seleniumboot.reporting;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads and manages {@link ReportAdapter} instances.
 *
 * <p>The built-in {@link HtmlReportAdapter} is always registered first.
 * SPI adapters are appended after it; programmatic adapters can be added
 * via {@link #register(ReportAdapter)} before suite finish.
 */
public final class ReportAdapterRegistry {

    private static final List<ReportAdapter> adapters = new ArrayList<>();
    private static boolean loaded = false;

    private ReportAdapterRegistry() {}

    /**
     * Registers the built-in HTML adapter and discovers all SPI adapters.
     * Safe to call multiple times — only loads once.
     */
    public static synchronized void loadAll() {
        if (loaded) return;
        adapters.add(new HtmlReportAdapter());
        ServiceLoader<ReportAdapter> loader = ServiceLoader.load(ReportAdapter.class);
        for (ReportAdapter adapter : loader) {
            adapters.add(adapter);
            System.out.println("[Selenium Boot] ReportAdapter loaded: " + adapter.getName());
        }
        loaded = true;
    }

    /**
     * Programmatically adds a report adapter.
     * Call this before suite execution starts (e.g., in a plugin's {@code onLoad}).
     */
    public static synchronized void register(ReportAdapter adapter) {
        adapters.add(adapter);
    }

    /**
     * Invokes {@link ReportAdapter#generate(File)} on every registered adapter.
     * Failures in one adapter are logged and do not prevent others from running.
     */
    public static void generateAll() {
        File metricsJson = new File("target/selenium-boot-metrics.json");
        for (ReportAdapter adapter : adapters) {
            try {
                adapter.generate(metricsJson);
            } catch (Exception e) {
                System.err.println(
                    "[Selenium Boot] ReportAdapter [" + adapter.getName() + "] failed: " + e.getMessage()
                );
            }
        }
    }
}
