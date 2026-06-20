package com.seleniumboot.reporting;

import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates a JUnit-compatible XML report (surefire format) from execution metrics.
 *
 * <p>The output directory is resolved in the following order:
 * <ol>
 *   <li>System property {@code seleniumboot.reports.dir} — override for any build tool</li>
 *   <li>Gradle layout — {@code build/test-results/test/} when {@code build/} exists and
 *       {@code target/} does not</li>
 *   <li>Maven layout — {@code target/surefire-reports/} (default)</li>
 * </ol>
 *
 * <p>CI systems (Jenkins, GitHub Actions, GitLab CI) can parse results natively without
 * additional plugins.
 */
public final class JUnitXmlReporter {

    static String resolveOutputDir() {
        String override = System.getProperty("seleniumboot.reports.dir");
        if (override != null && !override.isBlank()) return override.trim();
        if (new File("build").exists() && !new File("target").exists()) {
            return "build/test-results/test";
        }
        return "target/surefire-reports";
    }

    private static String outputPath() {
        return resolveOutputDir() + "/TEST-SeleniumBoot.xml";
    }

    private JUnitXmlReporter() {}

    public static void export(Collection<TestTiming> timings, long totalDurationMs) {
        String dir = resolveOutputDir();
        File outputDir = new File(dir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Check whether any timing has a browser tag (= matrix run)
        boolean isMatrixRun = timings.stream().anyMatch(t -> t.getBrowser() != null);

        if (isMatrixRun) {
            // Group timings by browser and write one XML file per browser
            Map<String, List<TestTiming>> byBrowser = new LinkedHashMap<>();
            for (TestTiming t : timings) {
                String browser = t.getBrowser() != null ? t.getBrowser().toLowerCase() : "unknown";
                byBrowser.computeIfAbsent(browser, k -> new ArrayList<>()).add(t);
            }
            for (Map.Entry<String, List<TestTiming>> entry : byBrowser.entrySet()) {
                String browser = entry.getKey();
                List<TestTiming> browserTimings = entry.getValue();
                long browserDuration = browserTimings.stream()
                        .mapToLong(TestTiming::getTotalTime).sum();
                String path = dir + "/TEST-selenium-boot-" + browser + ".xml";
                writeXml(browserTimings, browserDuration, "SeleniumBoot-" + capitalize(browser), path);
            }
        }

        // Always write the combined report so CI tools that look for the default file still work
        writeXml(timings, totalDurationMs, "SeleniumBoot", outputPath());
    }

    private static void writeXml(Collection<TestTiming> timings, long totalDurationMs,
                                  String suiteName, String outputPath) {
        int total   = timings.size();
        long failed  = timings.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long skipped = timings.stream().filter(t -> "SKIPPED".equals(t.getStatus())).count();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format(
                "<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" "
                + "skipped=\"%d\" errors=\"0\" time=\"%.3f\">\n",
                escapeXml(suiteName), total, failed, skipped, totalDurationMs / 1000.0));

        for (TestTiming t : timings) {
            String status  = t.getStatus() != null ? t.getStatus() : "UNKNOWN";
            double seconds = t.getTotalTime() / 1000.0;

            xml.append(String.format(
                    "  <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\"",
                    escapeXml(t.getTestId()), escapeXml(suiteName), seconds));

            switch (status) {
                case "FAILED":
                    String msg = t.getErrorMessage() != null ? t.getErrorMessage() : "Test failed";
                    String trace = t.getStackTrace() != null ? t.getStackTrace() : t.getTestId() + " FAILED";
                    xml.append(">\n");
                    xml.append("    <failure message=\"").append(escapeXml(msg)).append("\">")
                       .append(escapeXml(trace))
                       .append("</failure>\n");
                    xml.append("  </testcase>\n");
                    break;
                case "SKIPPED":
                    xml.append(">\n");
                    xml.append("    <skipped/>\n");
                    xml.append("  </testcase>\n");
                    break;
                default:
                    xml.append("/>\n");
                    break;
            }
        }

        xml.append("</testsuite>\n");

        try (FileWriter writer = new FileWriter(new File(outputPath))) {
            writer.write(xml.toString());
            System.out.println("[Selenium Boot] JUnit XML report → " + outputPath);
        } catch (IOException e) {
            System.err.println("[Selenium Boot] Failed to write JUnit XML report: "
                    + e.getMessage());
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
