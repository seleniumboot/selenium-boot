package com.seleniumboot.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public final class HtmlReportGenerator {

    private HtmlReportGenerator() {}

    public static void generate() {

        try {

            File jsonFile =
                    new File("target/selenium-boot-metrics.json");

            if (!jsonFile.exists()) {
                System.err.println(
                        "[Selenium Boot] Metrics JSON not found. Skipping HTML report."
                );
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonFile);

            String html = buildHtml(root);

            File reportFile =
                    new File("target/selenium-boot-report.html");

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(html);
            }

            System.out.println(
                    "[Selenium Boot] HTML report generated at target/selenium-boot-report.html"
            );

        } catch (Exception e) {
            System.err.println(
                    "[Selenium Boot] HTML report generation failed: "
                            + e.getMessage()
            );
        }
    }

    private static String buildMetadataSection(JsonNode root) {
        String profile = System.getProperty("selenium.boot.profile", "default");
        String buildNumber = System.getenv().getOrDefault("BUILD_NUMBER", "local");
        String branch = System.getenv().getOrDefault("GIT_BRANCH", "local");
        String commit = System.getenv().getOrDefault("GIT_COMMIT", "unknown");

        SeleniumBootConfig config = SeleniumBootContext.getConfig();

        SeleniumBootConfig.Browser browserCfg = config.getBrowser();
        SeleniumBootConfig.Execution executionCfg = config.getExecution();
        SeleniumBootConfig.Retry retryCfg = config.getRetry();
        SeleniumBootConfig.Timeouts timeoutsCfg = config.getTimeouts();

        String browser = browserCfg != null ? browserCfg.getName() : "unknown";
        boolean headless = browserCfg != null && browserCfg.isHeadless();
        String executionMode = executionCfg != null ? executionCfg.getMode() : "unknown";
        String baseUrl = executionCfg != null ? executionCfg.getBaseUrl() : null;
        String gridUrl = executionCfg != null ? executionCfg.getGridUrl() : null;
        String parallel = executionCfg != null ? executionCfg.getParallel() : "none";
        int threadCount = executionCfg != null ? executionCfg.getThreadCount() : 1;
        int maxSessions = executionCfg != null ? executionCfg.getMaxActiveSessions() : 5;
        boolean retryEnabled = retryCfg != null && retryCfg.isEnabled();
        int maxAttempts = retryCfg != null ? retryCfg.getMaxAttempts() : 1;
        int explicitTimeout = timeoutsCfg != null ? timeoutsCfg.getExplicit() : 10;
        int pageLoadTimeout = timeoutsCfg != null ? timeoutsCfg.getPageLoad() : 30;

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card metadata-card\" style=\"margin-bottom:24px;\">\n");
        sb.append("  <div class=\"card-header\">Build Metadata</div>\n");
        sb.append("  <div class=\"card-body\">\n");
        sb.append("    <div class=\"meta-grid\">\n");

        appendMetaItem(sb, "Profile", profile);
        appendMetaItem(sb, "Execution Mode", executionMode);
        appendMetaItem(sb, "Browser", browser + (headless ? " (headless)" : ""));
        appendMetaItem(sb, "Base URL", baseUrl != null ? baseUrl : "—");
        appendMetaItem(sb, "Grid URL", gridUrl != null ? gridUrl : "—");
        appendMetaItem(sb, "Parallel", parallel);
        appendMetaItem(sb, "Thread Count", String.valueOf(threadCount));
        appendMetaItem(sb, "Max Sessions", String.valueOf(maxSessions));
        appendMetaItem(sb, "Retry", retryEnabled ? "Enabled (max " + maxAttempts + ")" : "Disabled");
        appendMetaItem(sb, "Explicit Timeout", explicitTimeout + "s");
        appendMetaItem(sb, "Page Load Timeout", pageLoadTimeout + "s");
        appendMetaItem(sb, "Build Number", buildNumber);
        appendMetaItem(sb, "Branch", branch);
        appendMetaItem(sb, "Commit", commit);
        appendMetaItem(sb, "Generated At", timestamp);

        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        return sb.toString();
    }

    private static String buildScreenshotCell(JsonNode test) {
        if (!test.has("screenshotPath")) {
            return "<span style=\"color:#bdbdbd\">—</span>";
        }
        File screenshotFile = new File(test.get("screenshotPath").asText());
        if (!screenshotFile.exists()) {
            return "<span style=\"color:#bdbdbd\">—</span>";
        }
        try {
            byte[] bytes = Files.readAllBytes(screenshotFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "<img src=\"data:image/png;base64," + base64 + "\" " +
                   "style=\"max-width:160px;max-height:90px;border-radius:4px;" +
                   "border:1px solid #e0e0e0;cursor:pointer\" " +
                   "onclick=\"this.style.maxWidth=this.style.maxWidth==='none'?'160px':'none'\" " +
                   "title=\"Click to expand\" />";
        } catch (Exception e) {
            return "<span style=\"color:#e53935\">load error</span>";
        }
    }

    private static void appendMetaItem(StringBuilder sb, String label, String value) {
        sb.append("      <div class=\"meta-item\">\n");
        sb.append("        <span class=\"meta-label\">").append(label).append("</span>\n");
        sb.append("        <span class=\"meta-value\">").append(value).append("</span>\n");
        sb.append("      </div>\n");
    }

    private static String buildHtml(JsonNode root) {

        String executionPercentiles =
                root.has("executionPercentilesMs")
                        ? root.get("executionPercentilesMs").toString()
                        : "{}";

        String driverPercentiles =
                root.has("driverStartupPercentilesMs")
                        ? root.get("driverStartupPercentilesMs").toString()
                        : "{}";

        String metadataSection = buildMetadataSection(root);

        StringBuilder rows = new StringBuilder();

        for (JsonNode test : root.get("tests")) {

            String status = test.has("status") ? test.get("status").asText() : "UNKNOWN";
            String statusClass = "status-" + status.toLowerCase();

            String screenshotCell = buildScreenshotCell(test);

            rows.append("<tr>")
                    .append("<td class=\"test-id\">").append(test.get("testId").asText()).append("</td>")
                    .append("<td>").append(test.get("thread").asText()).append("</td>")
                    .append("<td><span class=\"status-badge ").append(statusClass).append("\">")
                    .append(status).append("</span></td>")
                    .append("<td class=\"numeric\">").append(test.get("driverStartupMs").asLong()).append("</td>")
                    .append("<td class=\"numeric\">").append(test.get("testLogicMs").asLong()).append("</td>")
                    .append("<td class=\"numeric\">").append(test.get("totalMs").asLong()).append("</td>")
                    .append("<td>").append(screenshotCell).append("</td>")
                    .append("</tr>");
        }

        int totalTests    = root.has("totalTests")    ? root.get("totalTests").asInt()    : 0;
        int passedTests   = root.has("passedTests")   ? root.get("passedTests").asInt()   : 0;
        int failedTests   = root.has("failedTests")   ? root.get("failedTests").asInt()   : 0;
        int skippedTests  = root.has("skippedTests")  ? root.get("skippedTests").asInt()  : 0;
        long totalTimeMs   = root.has("totalTimeMs")   ? root.get("totalTimeMs").asLong()  : 0L;
        long averageTimeMs = root.has("averageTimeMs") ? root.get("averageTimeMs").asLong(): 0L;

        String template = loadTemplate();

        return template
                .replace("{{METADATA}}", metadataSection)
                .replace("{{PASSED}}", String.valueOf(passedTests))
                .replace("{{FAILED}}", String.valueOf(failedTests))
                .replace("{{SKIPPED}}", String.valueOf(skippedTests))
                .replace("{{TOTAL_TESTS}}", String.valueOf(totalTests))
                .replace("{{TOTAL_TIME_MS}}", String.valueOf(totalTimeMs))
                .replace("{{AVG_TIME_MS}}", String.valueOf(averageTimeMs))
                .replace("{{ROWS}}", rows.toString())
                .replace("{{EXECUTION_PERCENTILES}}", executionPercentiles.replace("'", "\\'"))
                .replace("{{DRIVER_PERCENTILES}}", driverPercentiles.replace("'", "\\'"));
    }

    private static String loadTemplate() {
        try (InputStream is = HtmlReportGenerator.class
                .getClassLoader()
                .getResourceAsStream("report-template.html")) {
            if (is == null) {
                throw new IllegalStateException("report-template.html not found in classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to load report-template.html", e);
        }
    }
}