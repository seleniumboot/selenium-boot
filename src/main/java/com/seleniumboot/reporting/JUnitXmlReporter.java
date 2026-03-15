package com.seleniumboot.reporting;

import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * Generates a JUnit-compatible XML report (surefire format) from execution metrics.
 *
 * The output is written to {@code target/surefire-reports/TEST-SeleniumBoot.xml}
 * so CI systems (Jenkins, GitHub Actions, GitLab CI) can parse test results
 * natively without additional plugins.
 */
public final class JUnitXmlReporter {

    private static final String OUTPUT_PATH =
            "target/surefire-reports/TEST-SeleniumBoot.xml";

    private JUnitXmlReporter() {}

    public static void export(Collection<TestTiming> timings, long totalDurationMs) {
        File outputDir = new File("target/surefire-reports");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int total   = timings.size();
        long passed  = timings.stream().filter(t -> "PASSED".equals(t.getStatus())).count();
        long failed  = timings.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        long skipped = timings.stream().filter(t -> "SKIPPED".equals(t.getStatus())).count();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format(
                "<testsuite name=\"SeleniumBoot\" tests=\"%d\" failures=\"%d\" "
                + "skipped=\"%d\" errors=\"0\" time=\"%.3f\">\n",
                total, failed, skipped, totalDurationMs / 1000.0));

        for (TestTiming t : timings) {
            String status  = t.getStatus() != null ? t.getStatus() : "UNKNOWN";
            double seconds = t.getTotalTime() / 1000.0;

            xml.append(String.format(
                    "  <testcase name=\"%s\" classname=\"SeleniumBoot\" time=\"%.3f\"",
                    escapeXml(t.getTestId()), seconds));

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

        try (FileWriter writer = new FileWriter(new File(OUTPUT_PATH))) {
            writer.write(xml.toString());
            System.out.println("[Selenium Boot] JUnit XML report → " + OUTPUT_PATH);
        } catch (IOException e) {
            System.err.println("[Selenium Boot] Failed to write JUnit XML report: "
                    + e.getMessage());
        }
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
