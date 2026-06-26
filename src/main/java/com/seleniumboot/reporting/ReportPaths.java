package com.seleniumboot.reporting;

import java.io.File;

/**
 * Resolves the output locations for the metrics JSON, HTML report, and metrics
 * history so that multiple test engines running in one build (e.g. a TestNG
 * suite via Surefire <em>and</em> JUnit 5 via Failsafe) don't overwrite each
 * other's report.
 *
 * <p>The base directory honors the system property {@code seleniumboot.reports.dir}
 * — the same switch used by {@link JUnitXmlReporter}. Point each engine's run at a
 * distinct directory and each gets its own self-contained report:
 *
 * <pre>{@code
 * <!-- in the JUnit 5 (Failsafe) execution -->
 * <systemPropertyVariables>
 *   <seleniumboot.reports.dir>target/junit5</seleniumboot.reports.dir>
 * </systemPropertyVariables>
 * }</pre>
 *
 * <p>When the property is unset the base directory defaults to {@code target}
 * (or {@code build} for a Gradle layout), preserving the historical paths.
 *
 * @since 3.1.1
 */
public final class ReportPaths {

    private ReportPaths() {}

    /** Base directory for metrics + HTML report; honors {@code seleniumboot.reports.dir}. */
    public static String baseDir() {
        String override = System.getProperty("seleniumboot.reports.dir");
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        if (new File("build").exists() && !new File("target").exists()) {
            return "build";
        }
        return "target";
    }

    /** {@code <baseDir>/selenium-boot-metrics.json} — the report's data source. */
    public static File metricsJson() {
        return new File(baseDir(), "selenium-boot-metrics.json");
    }

    /** {@code <baseDir>/selenium-boot-report.html} — the generated HTML report. */
    public static File htmlReport() {
        return new File(baseDir(), "selenium-boot-report.html");
    }

    /** {@code <baseDir>/metrics-history} — timestamped metrics copies for flakiness analysis. */
    public static File metricsHistoryDir() {
        return new File(baseDir(), "metrics-history");
    }
}
