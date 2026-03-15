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
            return "<span class=\"no-screenshot\">—</span>";
        }
        File screenshotFile = new File(test.get("screenshotPath").asText());
        if (!screenshotFile.exists()) {
            return "<span class=\"no-screenshot\">—</span>";
        }
        try {
            byte[] bytes = Files.readAllBytes(screenshotFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String src = "data:image/png;base64," + base64;
            return "<img src=\"" + src + "\" class=\"screenshot-thumb\" "
                   + "onclick=\"openLightbox(this.src)\" title=\"Click to view full size\" />";
        } catch (java.io.IOException e) {
            return "<span class=\"screenshot-error\">load error</span>";
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

        String metadataSection = buildMetadataSection(root);

        int totalTests    = root.has("totalTests")    ? root.get("totalTests").asInt()    : 0;
        int passedTests   = root.has("passedTests")   ? root.get("passedTests").asInt()   : 0;
        int failedTests   = root.has("failedTests")   ? root.get("failedTests").asInt()   : 0;
        int skippedTests  = root.has("skippedTests")  ? root.get("skippedTests").asInt()  : 0;
        long totalTimeMs   = root.has("totalTimeMs")   ? root.get("totalTimeMs").asLong()  : 0L;
        long averageTimeMs = root.has("averageTimeMs") ? root.get("averageTimeMs").asLong(): 0L;
        double passRate    = root.has("passRate")       ? root.get("passRate").asDouble()   : 0.0;
        int flakyTests     = root.has("flakyTests")     ? root.get("flakyTests").asInt()    : 0;
        int recoveredTests = root.has("recoveredTests") ? root.get("recoveredTests").asInt(): 0;

        String passRateClass = passRate >= 80 ? "rate-good" : passRate >= 60 ? "rate-warn" : "rate-bad";
        String passRateStr   = String.format("%.1f", passRate);

        String donutData = String.format(
                "{\"passed\":%d,\"failed\":%d,\"skipped\":%d}", passedTests, failedTests, skippedTests);

        JsonNode tests = root.has("tests") ? root.get("tests") : null;

        StringBuilder rows = new StringBuilder();
        if (tests != null) {
            // Group tests by class name preserving insertion order
            java.util.Map<String, java.util.List<JsonNode>> byClass = new java.util.LinkedHashMap<>();
            for (JsonNode test : tests) {
                String cls = test.has("testClassName") ? test.get("testClassName").asText() : "";
                if (cls.isEmpty()) cls = "Unknown";
                byClass.computeIfAbsent(cls, k -> new java.util.ArrayList<>()).add(test);
            }
            int rowIndex = 0;
            for (java.util.Map.Entry<String, java.util.List<JsonNode>> entry : byClass.entrySet()) {
                String groupId  = entry.getKey();
                java.util.List<JsonNode> members = entry.getValue();
                long groupPassed  = members.stream().filter(t -> "PASSED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                long groupFailed  = members.stream().filter(t -> "FAILED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                long groupSkipped = members.stream().filter(t -> "SKIPPED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                rows.append(buildGroupHeader(groupId, members.size(), groupPassed, groupFailed, groupSkipped, true));
                for (JsonNode test : members) {
                    rows.append(buildRow(test, rowIndex++, groupId, true));
                }
            }
        }

        String retrySection   = buildRetrySection(flakyTests, recoveredTests);
        String slowestSection = tests != null ? buildSlowestTests(tests) : "";
        String failureRows    = buildFailureRows(tests);
        String failureBadge   = failedTests > 0
                ? "<span class=\"nav-count nav-count-fail\">" + failedTests + "</span>"
                : "";

        String template = loadTemplate();

        return template
                .replace("{{METADATA}}", metadataSection)
                .replace("{{PASSED}}", String.valueOf(passedTests))
                .replace("{{FAILED}}", String.valueOf(failedTests))
                .replace("{{SKIPPED}}", String.valueOf(skippedTests))
                .replace("{{TOTAL_TESTS}}", String.valueOf(totalTests))
                .replace("{{TOTAL_TIME_MS}}", String.valueOf(totalTimeMs))
                .replace("{{AVG_TIME_MS}}", String.valueOf(averageTimeMs))
                .replace("{{PASS_RATE}}", passRateStr)
                .replace("{{PASS_RATE_CLASS}}", passRateClass)
                .replace("{{FLAKY_TESTS}}", String.valueOf(flakyTests))
                .replace("{{RECOVERED_TESTS}}", String.valueOf(recoveredTests))
                .replace("{{RETRY_SECTION}}", retrySection)
                .replace("{{SLOWEST_TESTS}}", slowestSection)
                .replace("{{DONUT_DATA}}", donutData)
                .replace("{{ROWS}}", rows.toString())
                .replace("{{FAILURE_ROWS}}", failureRows)
                .replace("{{FAILURE_BADGE}}", failureBadge)
                .replace("{{EXECUTION_PERCENTILES}}", executionPercentiles.replace("'", "\\'"));
    }

    private static String buildGroupHeader(String groupId, int total, long passed, long failed, long skipped, boolean collapsed) {
        String groupKey = escapeHtml(groupId);
        String badges = "";
        if (passed  > 0) badges += "<span class=\"status-badge status-passed\">"  + passed  + " passed</span> ";
        if (failed  > 0) badges += "<span class=\"status-badge status-failed\">"  + failed  + " failed</span> ";
        if (skipped > 0) badges += "<span class=\"status-badge status-skipped\">" + skipped + " skipped</span>";
        String iconClass = collapsed ? "group-icon closed" : "group-icon";
        return "<tr class=\"group-header\" data-group=\"" + groupKey + "\" onclick=\"toggleGroup('" + groupKey + "')\">"
                + "<td colspan=\"7\">"
                + "<span class=\"" + iconClass + "\" id=\"gicon-" + groupKey + "\">&#x25BC;</span> "
                + "<strong>" + groupKey + "</strong>"
                + "<span class=\"group-count\"> &mdash; " + total + " test" + (total != 1 ? "s" : "") + "</span>"
                + " &nbsp;" + badges
                + "</td>"
                + "</tr>";
    }

    private static String buildRow(JsonNode test, int rowIndex, String groupId, boolean collapsed) {
        String status      = test.has("status")      ? test.get("status").asText()      : "UNKNOWN";
        String statusClass = "status-" + status.toLowerCase();
        String rawTestId   = test.has("testId")      ? test.get("testId").asText()      : "";
        int lastDot        = rawTestId.lastIndexOf('.');
        String methodName  = escapeHtml(lastDot >= 0 ? rawTestId.substring(lastDot + 1) : rawTestId);
        String description = test.has("description") ? escapeHtml(test.get("description").asText()) : "";
        long   logicMs     = test.has("testLogicMs") ? test.get("testLogicMs").asLong() : 0L;
        long   totalMs     = test.has("totalMs")     ? test.get("totalMs").asLong()     : 0L;
        int    retryCount  = test.has("retryCount")  ? test.get("retryCount").asInt()   : 0;
        String errorMsg    = test.has("errorMessage") ? test.get("errorMessage").asText() : null;
        String stackTrace  = test.has("stackTrace")   ? test.get("stackTrace").asText()  : null;
        String screenshotCell = buildScreenshotCell(test);
        String groupKey    = escapeHtml(groupId);

        String retryBadge = retryCount > 0
                ? "<span class=\"retry-badge\">&#x21bb; " + retryCount + "x</span> "
                : "";

        String stepsHtml   = buildStepTimeline(test);
        boolean hasDetail  = errorMsg != null || stackTrace != null || !stepsHtml.isEmpty();
        String detailRow   = "";
        if (hasDetail) {
            String errorHtml = errorMsg   != null ? "<div class=\"error-msg\">"   + escapeHtml(errorMsg)   + "</div>" : "";
            String traceHtml = stackTrace != null ? "<pre class=\"stack-trace\">" + escapeHtml(stackTrace) + "</pre>" : "";
            String stepsSection = !stepsHtml.isEmpty()
                    ? "<div class=\"step-timeline-section\"><div class=\"step-timeline-header\">Steps (" + test.get("steps").size() + ")</div>"
                      + "<div class=\"step-timeline\">" + stepsHtml + "</div></div>"
                    : "";
            String detailDisplay = collapsed ? " style=\"display:none\"" : "";
            String iconOpen      = collapsed ? "" : " open";
            detailRow = "<tr class=\"detail-row group-member\" data-group=\"" + groupKey + "\" id=\"detail-" + rowIndex + "\"" + detailDisplay + ">"
                    + "<td colspan=\"7\"><div class=\"detail-panel\">" + stepsSection + errorHtml + traceHtml + "</div></td>"
                    + "</tr>";
        }

        String rowClass    = "group-member" + (hasDetail ? " expandable" : "");
        String clickAttr   = hasDetail ? " onclick=\"toggleDetail(" + rowIndex + ")\"" : "";
        String memberStyle = collapsed ? " style=\"display:none\"" : "";
        String iconOpen    = (!collapsed && hasDetail) ? " open" : "";

        return "<tr class=\"" + rowClass + "\""
                + clickAttr
                + memberStyle
                + " data-group=\"" + groupKey + "\""
                + " data-status=\"" + status + "\""
                + " data-test=\"" + methodName.toLowerCase() + "\">"
                + "<td class=\"test-id\">" + retryBadge + methodName + "</td>"
                + "<td class=\"desc-cell\">" + description + "</td>"
                + "<td><span class=\"status-badge " + statusClass + "\">" + status + "</span></td>"
                + "<td class=\"numeric\">" + logicMs + "</td>"
                + "<td class=\"numeric\">" + totalMs + "</td>"
                + "<td>" + screenshotCell + "</td>"
                + "<td>" + (hasDetail ? "<span class=\"expand-icon" + iconOpen + "\" id=\"icon-" + rowIndex + "\">&#x25BC;</span>" : "") + "</td>"
                + "</tr>"
                + detailRow;
    }

    private static String buildFailureRows(JsonNode tests) {
        if (tests == null) return noFailuresRow();
        java.util.Map<String, java.util.List<JsonNode>> byClass = new java.util.LinkedHashMap<>();
        for (JsonNode test : tests) {
            if (!"FAILED".equals(test.has("status") ? test.get("status").asText() : "")) continue;
            String cls = test.has("testClassName") ? test.get("testClassName").asText() : "";
            if (cls.isEmpty()) cls = "Unknown";
            byClass.computeIfAbsent(cls, k -> new java.util.ArrayList<>()).add(test);
        }
        if (byClass.isEmpty()) return noFailuresRow();

        StringBuilder rows = new StringBuilder();
        int rowIndex = 50000; // offset avoids ID conflicts with the test-cases table
        for (java.util.Map.Entry<String, java.util.List<JsonNode>> entry : byClass.entrySet()) {
            String groupKey = escapeHtml(entry.getKey());
            int total = entry.getValue().size();
            // Non-collapsible group header for failures (always expanded)
            rows.append("<tr class=\"group-header\">"
                    + "<td colspan=\"7\">"
                    + "<span class=\"group-icon\">&#x25BC;</span> "
                    + "<strong>" + groupKey + "</strong>"
                    + "<span class=\"group-count\"> &mdash; " + total + " failure" + (total != 1 ? "s" : "") + "</span>"
                    + "</td></tr>");
            for (JsonNode test : entry.getValue()) {
                rows.append(buildRow(test, rowIndex++, entry.getKey(), false));
            }
        }
        return rows.toString();
    }

    private static String noFailuresRow() {
        return "<tr><td colspan=\"7\" class=\"no-failures-msg\">&#10003; No failures in this run</td></tr>";
    }

    private static String buildRetrySection(int flakyTests, int recoveredTests) {
        if (flakyTests == 0) return "";
        int stillFailing = flakyTests - recoveredTests;
        return "<div class=\"card section-mb\">"
                + "<div class=\"card-header\">Retry Summary</div>"
                + "<div class=\"card-body\">"
                + "<div class=\"summary-row retry-summary-row\">"
                + "<div class=\"card stat-card\"><div class=\"stat-value stat-value-warn\">" + flakyTests + "</div><div class=\"stat-label\">Retried</div></div>"
                + "<div class=\"card stat-card\"><div class=\"stat-value stat-value-good\">" + recoveredTests + "</div><div class=\"stat-label\">Recovered</div></div>"
                + "<div class=\"card stat-card\"><div class=\"stat-value stat-value-bad\">"  + stillFailing + "</div><div class=\"stat-label\">Still Failing</div></div>"
                + "</div></div></div>";
    }

    private static String buildSlowestTests(JsonNode tests) {
        java.util.List<JsonNode> list = new java.util.ArrayList<>();
        tests.forEach(list::add);
        list.sort((a, b) -> Long.compare(
                b.has("totalMs") ? b.get("totalMs").asLong() : 0L,
                a.has("totalMs") ? a.get("totalMs").asLong() : 0L));
        int count = Math.min(5, list.size());
        long max = count > 0 && list.get(0).has("totalMs") ? list.get(0).get("totalMs").asLong() : 1L;
        if (max == 0) max = 1L;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            JsonNode t = list.get(i);
            String rawId = t.has("testId") ? t.get("testId").asText() : "unknown";
            int dot      = rawId.lastIndexOf('.');
            String name  = escapeHtml(dot >= 0 ? rawId.substring(dot + 1) : rawId);
            long   ms    = t.has("totalMs") ? t.get("totalMs").asLong() : 0L;
            int    pct   = (int) (ms * 100 / max);
            sb.append("<div class=\"slow-item\">")
              .append("<span class=\"slow-name\">").append(name).append("</span>")
              .append("<div class=\"slow-bar-wrap\"><div class=\"slow-bar\" style=\"width:").append(pct).append("%\"></div></div>")
              .append("<span class=\"slow-ms\">").append(ms).append(" ms</span>")
              .append("</div>");
        }
        return sb.toString();
    }

    private static String buildStepTimeline(JsonNode test) {
        if (!test.has("steps") || test.get("steps").size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (JsonNode step : test.get("steps")) {
            String name     = step.has("name")     ? escapeHtml(step.get("name").asText())   : "";
            String status   = step.has("status")   ? step.get("status").asText().toUpperCase() : "INFO";
            long   offsetMs = step.has("offsetMs") ? step.get("offsetMs").asLong()            : 0L;
            String badgeClass = "step-badge-info";
            if ("PASS".equals(status)) badgeClass = "step-badge-pass";
            else if ("FAIL".equals(status)) badgeClass = "step-badge-fail";

            String thumbHtml = "";
            if (step.has("screenshotBase64")) {
                String src = "data:image/png;base64," + step.get("screenshotBase64").asText();
                thumbHtml = "<img src=\"" + src + "\" class=\"step-thumb\" "
                          + "onclick=\"openLightbox(this.src)\" title=\"Click to view full size\" />";
            }

            sb.append("<div class=\"step-item\">")
              .append("<span class=\"step-num\">").append(i++).append("</span>")
              .append("<span class=\"step-name\">").append(name).append("</span>")
              .append("<span class=\"step-offset\">+").append(offsetMs).append("ms</span>")
              .append("<span class=\"step-badge ").append(badgeClass).append("\">").append(status).append("</span>")
              .append(thumbHtml)
              .append("</div>");
        }
        return sb.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    private static String safeStr(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText("") : "";
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