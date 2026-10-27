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

            File jsonFile = ReportPaths.metricsJson();

            if (!jsonFile.exists()) {
                System.err.println(
                        "[Selenium Boot] Metrics JSON not found. Skipping HTML report."
                );
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonFile);

            String html = buildHtml(root);

            File reportFile = ReportPaths.htmlReport();
            File reportDir = reportFile.getParentFile();
            if (reportDir != null && !reportDir.exists()) {
                reportDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(html);
            }

            System.out.println(
                    "[Selenium Boot] HTML report generated at " + reportFile.getPath()
            );

        } catch (Exception e) {
            System.err.println(
                    "[Selenium Boot] HTML report generation failed: "
                            + e.getMessage()
            );
        }
    }

    /** @return {run summary line, collapsed run-configuration block} */
    private static String[] buildMetadataSection(JsonNode root) {
        String profile = System.getProperty("selenium.boot.profile", "default");

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

        // Ordered so the one-line summary reads naturally; empty values are dropped.
        java.util.Map<String, String> meta = new java.util.LinkedHashMap<>();
        meta.put("Browser", browser + (headless ? " (headless)" : ""));
        meta.put("Execution Mode", executionMode);
        meta.put("Parallel", parallel);
        meta.put("Thread Count", String.valueOf(threadCount));
        meta.put("Retry", retryEnabled ? "Enabled (max " + maxAttempts + ")" : "Disabled");
        meta.put("Profile", profile);
        meta.put("Base URL", baseUrl);
        meta.put("Grid URL", gridUrl);
        meta.put("Max Sessions", String.valueOf(maxSessions));
        meta.put("Explicit Timeout", explicitTimeout + "s");
        meta.put("Page Load Timeout", pageLoadTimeout + "s");
        meta.put("Build Number", ciValue("BUILD_NUMBER"));
        meta.put("Branch", ciValue("GIT_BRANCH"));
        meta.put("Commit", ciValue("GIT_COMMIT"));
        meta.put("Generated At", timestamp);
        meta.values().removeIf(v -> v == null || v.isBlank());

        StringBuilder summary = new StringBuilder();
        for (String key : new String[]{"Browser", "Execution Mode", "Thread Count", "Retry", "Branch", "Generated At"}) {
            String v = meta.get(key);
            if (v == null) continue;
            if (summary.length() > 0) summary.append(" &middot; ");
            summary.append("Thread Count".equals(key) ? escapeHtml(v) + " threads"
                         : "Retry".equals(key) ? "retry " + escapeHtml(v.toLowerCase())
                         : escapeHtml(v));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<details class=\"card metadata-card\" style=\"margin-bottom:24px;\">\n");
        sb.append("  <summary class=\"card-header\">Run configuration</summary>\n");
        sb.append("  <div class=\"card-body\">\n");
        sb.append("    <div class=\"meta-grid\">\n");
        meta.forEach((k, v) -> appendMetaItem(sb, k, escapeHtml(v)));
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</details>\n");

        return new String[]{"<div class=\"run-summary\">" + summary + "</div>\n", sb.toString()};
    }

    /** CI-provided value, or null when not running under CI (so the row is omitted). */
    private static String ciValue(String env) {
        String v = System.getenv(env);
        return v == null || v.isBlank() ? null : v;
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

    /** hh:mm:ss; hours are not capped at 24. */
    static String formatDuration(long ms) {
        long totalSeconds = Math.max(0, ms) / 1000;
        return String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    /**
     * Pass-rate and duration of the last {@code maxRuns} runs from the metrics history, oldest
     * first, as a JSON array for the trend charts. Empty array when there is no history.
     */
    static String buildTrendData(int maxRuns) {
        File dir = ReportPaths.metricsHistoryDir();
        File[] files = dir.isDirectory() ? dir.listFiles((d, n) -> n.endsWith(".json")) : null;
        if (files == null || files.length == 0) return "[]";
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        int from = Math.max(0, files.length - maxRuns);
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ArrayNode out = mapper.createArrayNode();
        for (int i = from; i < files.length; i++) {
            try {
                JsonNode n = mapper.readTree(files[i]);
                com.fasterxml.jackson.databind.node.ObjectNode run = out.addObject();
                String name = files[i].getName().replaceAll("^.*metrics-|\\.json$", "");
                run.put("label", name.length() >= 13
                        ? name.substring(4, 6) + "-" + name.substring(6, 8) + " " + name.substring(9, 11) + ":" + name.substring(11, 13)
                        : name);
                run.put("passRate", n.path("passRate").asDouble(0));
                run.put("failed", n.path("failedTests").asInt(0));
                run.put("flakyTests", n.path("flakyTests").asInt(0));
                long ms = n.path("suiteDurationMs").asLong(0);
                run.put("durationSec", Math.round((ms > 0 ? ms : n.path("totalTimeMs").asLong(0)) / 100.0) / 10.0);
            } catch (Exception ignored) {
                // unreadable history file: skip it
            }
        }
        return out.toString();
    }

    static String buildHtml(JsonNode root) {

        String executionPercentiles =
                root.has("executionPercentilesMs")
                        ? root.get("executionPercentilesMs").toString()
                        : "{}";

        String[] metadata = buildMetadataSection(root);

        int totalTests    = root.has("totalTests")    ? root.get("totalTests").asInt()    : 0;
        int passedTests   = root.has("passedTests")   ? root.get("passedTests").asInt()   : 0;
        int failedTests   = root.has("failedTests")   ? root.get("failedTests").asInt()   : 0;
        int skippedTests  = root.has("skippedTests")  ? root.get("skippedTests").asInt()  : 0;
        long totalTimeMs   = root.has("totalTimeMs")   ? root.get("totalTimeMs").asLong()  : 0L;
        // Wall-clock suite duration; older metrics JSON lacks it, so fall back to the summed test time.
        long suiteMs = root.has("suiteDurationMs") && root.get("suiteDurationMs").asLong() > 0
                ? root.get("suiteDurationMs").asLong() : totalTimeMs;
        long averageTimeMs = root.has("averageTimeMs") ? root.get("averageTimeMs").asLong(): 0L;
        double passRate    = root.has("passRate")       ? root.get("passRate").asDouble()   : 0.0;
        int flakyTests     = root.has("flakyTests")     ? root.get("flakyTests").asInt()    : 0;
        int recoveredTests = root.has("recoveredTests") ? root.get("recoveredTests").asInt(): 0;

        String passRateClass = passRate >= 80 ? "rate-good" : passRate >= 60 ? "rate-warn" : "rate-bad";
        String passRateStr   = String.format("%.1f", passRate);

        String donutData = String.format(
                "{\"passed\":%d,\"failed\":%d,\"skipped\":%d}", passedTests, failedTests, skippedTests);

        JsonNode tests = root.has("tests") ? root.get("tests") : null;

        // Detect matrix run: any test entry with a non-null "browser" field
        boolean isMatrixRun = false;
        if (tests != null) {
            for (JsonNode test : tests) {
                if (test.has("browser") && !test.get("browser").asText("").isEmpty()) {
                    isMatrixRun = true;
                    break;
                }
            }
        }

        StringBuilder rows = new StringBuilder();
        if (tests != null) {
            // Group tests by class name (+ browser when matrix) preserving insertion order
            java.util.Map<String, java.util.List<JsonNode>> byGroup = new java.util.LinkedHashMap<>();
            for (JsonNode test : tests) {
                String cls = test.has("testClassName") ? test.get("testClassName").asText() : "";
                if (cls.isEmpty()) cls = "Unknown";
                String groupKey = isMatrixRun
                        ? cls + " [" + capitalize(test.has("browser") ? test.get("browser").asText("") : "") + "]"
                        : cls;
                byGroup.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(test);
            }
            int rowIndex = 0;
            for (java.util.Map.Entry<String, java.util.List<JsonNode>> entry : byGroup.entrySet()) {
                String groupId  = entry.getKey();
                java.util.List<JsonNode> members = entry.getValue();
                long groupPassed  = members.stream().filter(t -> "PASSED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                long groupFailed  = members.stream().filter(t -> "FAILED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                long groupSkipped = members.stream().filter(t -> "SKIPPED".equals(t.has("status") ? t.get("status").asText() : "")).count();
                rows.append(buildGroupHeader(groupId, members.size(), groupPassed, groupFailed, groupSkipped, true, isMatrixRun));
                for (JsonNode test : members) {
                    rows.append(buildRow(test, rowIndex++, groupId, true, isMatrixRun));
                }
            }
        }

        String aiRunSummary     = root.hasNonNull("aiRunSummary")
                ? "<div class=\"card section-mb\"><div class=\"card-header\">&#x1F916; AI Run Summary</div>"
                  + "<div class=\"card-body ai-analysis-body\">" + escapeHtml(root.get("aiRunSummary").asText()) + "</div></div>"
                : "";
        String retrySection     = buildRetrySection(flakyTests, recoveredTests);
        String slowestSection   = tests != null ? buildSlowestTests(tests) : "";
        String timelineSection  = buildTimelineSection(tests);
        String tagResults       = tests != null ? buildTagResultsSection(tests) : "";
        String flakinessSection = buildFlakinessSection() + buildTestStatusSection(tests);
        String failureRows    = buildFailureRows(tests, isMatrixRun);
        String failureGroups  = buildFailureGroups(tests);
        String failureBadge   = failedTests > 0
                ? "<span class=\"nav-count nav-count-fail\">" + failedTests + "</span>"
                : "";

        String failuresShortcut = failedTests > 0
                ? "<div class=\"failures-shortcut\"><strong>" + failedTests + " failed.</strong> "
                  + "<a href=\"#\" onclick=\"showTab('failures', document.querySelector('.nav-item[onclick*=\\'failures\\']')); return false;\">View failures &rarr;</a></div>\n"
                : "";

        String template = loadTemplate();

        return template
                .replace("{{CHART_JS}}", loadResource("report/chart.umd.min.js"))
                .replace("{{REPORT_CSS}}", loadResource("report/report.css"))
                .replace("{{REPORT_JS}}", loadResource("report/report.js"))
                .replace("{{REPORT_MARKUP}}", loadMarkup())
                .replace("{{RUN_SUMMARY}}", metadata[0])
                .replace("{{METADATA}}", metadata[1])
                .replace("{{PASSED}}", String.valueOf(passedTests))
                .replace("{{FAILED}}", String.valueOf(failedTests))
                .replace("{{SKIPPED}}", String.valueOf(skippedTests))
                .replace("{{TOTAL_TESTS}}", String.valueOf(totalTests))
                .replace("{{TOTAL_TIME_MS}}", String.valueOf(totalTimeMs))
                .replace("{{SUITE_DURATION}}", formatDuration(suiteMs))
                .replace("{{AVG_TIME_MS}}", String.valueOf(averageTimeMs))
                .replace("{{PASS_RATE}}", passRateStr)
                .replace("{{PASS_RATE_CLASS}}", passRateClass)
                .replace("{{FLAKY_TESTS}}", String.valueOf(flakyTests))
                .replace("{{RECOVERED_TESTS}}", String.valueOf(recoveredTests))
                .replace("{{AI_RUN_SUMMARY}}", aiRunSummary)
                .replace("{{RETRY_SECTION}}", retrySection)
                .replace("{{SLOWEST_TESTS}}", slowestSection)
                .replace("{{TIMELINE_SECTION}}", timelineSection)
                .replace("{{TAG_RESULTS}}", tagResults)
                .replace("{{FLAKINESS_SECTION}}", flakinessSection)
                .replace("{{DONUT_DATA}}", donutData)
                .replace("{{TREND_DATA}}", buildTrendData(20))
                .replace("{{ROWS}}", rows.toString())
                .replace("{{FAILURE_ROWS}}", failureRows)
                .replace("{{FAILURE_GROUPS}}", failureGroups)
                .replace("{{FAILURE_BADGE}}", failureBadge)
                .replace("{{FAILURES_SHORTCUT}}", failuresShortcut)
                .replace("{{EXECUTION_PERCENTILES}}", executionPercentiles.replace("'", "\\'"));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String buildGroupHeader(String groupId, int total, long passed, long failed, long skipped, boolean collapsed) {
        return buildGroupHeader(groupId, total, passed, failed, skipped, collapsed, false);
    }

    private static String buildGroupHeader(String groupId, int total, long passed, long failed, long skipped, boolean collapsed, boolean showBrowser) {
        String groupKey = escapeHtml(groupId);
        String badges = "";
        if (passed  > 0) badges += "<span class=\"status-badge status-passed\">"  + passed  + " passed</span> ";
        if (failed  > 0) badges += "<span class=\"status-badge status-failed\">"  + failed  + " failed</span> ";
        if (skipped > 0) badges += "<span class=\"status-badge status-skipped\">" + skipped + " skipped</span>";
        String iconClass = collapsed ? "group-icon closed" : "group-icon";
        int colspan = showBrowser ? 8 : 7;
        return "<tr class=\"group-header\" data-group=\"" + groupKey + "\" onclick=\"toggleGroup('" + groupKey + "')\">"
                + "<td colspan=\"" + colspan + "\">"
                + "<span class=\"" + iconClass + "\" id=\"gicon-" + groupKey + "\">&#x25BC;</span> "
                + "<strong>" + groupKey + "</strong>"
                + "<span class=\"group-count\"> &mdash; " + total + " test" + (total != 1 ? "s" : "") + "</span>"
                + " &nbsp;" + badges
                + "</td>"
                + "</tr>";
    }

    private static String buildRow(JsonNode test, int rowIndex, String groupId, boolean collapsed) {
        return buildRow(test, rowIndex, groupId, collapsed, false);
    }

    private static String buildRow(JsonNode test, int rowIndex, String groupId, boolean collapsed, boolean showBrowser) {
        return buildRow(test, rowIndex, groupId, collapsed, collapsed, showBrowser);
    }

    /**
     * @param collapsed    hide the test row itself (its class group is collapsed)
     * @param detailHidden hide the expandable detail row under it
     */
    private static String buildRow(JsonNode test, int rowIndex, String groupId, boolean collapsed,
                                   boolean detailHidden, boolean showBrowser) {
        String status      = test.has("status")      ? test.get("status").asText()      : "UNKNOWN";
        String statusClass = "status-" + status.toLowerCase();
        String rawTestId   = test.has("testId")      ? test.get("testId").asText()      : "";
        int lastDot        = rawTestId.lastIndexOf('.');
        String methodName  = escapeHtml(lastDot >= 0 ? rawTestId.substring(lastDot + 1) : rawTestId);
        String description = test.has("description") ? escapeHtml(test.get("description").asText()) : "";
        long   logicMs     = test.has("testLogicMs") ? test.get("testLogicMs").asLong() : 0L;
        long   totalMs     = test.has("totalMs")     ? test.get("totalMs").asLong()     : 0L;
        int    retryCount  = test.has("retryCount")  ? test.get("retryCount").asInt()   : 0;
        String errorMsg      = test.has("errorMessage")  ? test.get("errorMessage").asText()  : null;
        String stackTrace    = test.has("stackTrace")    ? test.get("stackTrace").asText()    : null;
        String recordingPath = test.has("recordingPath") ? test.get("recordingPath").asText() : null;
        String tracePath     = test.has("tracePath")     ? test.get("tracePath").asText()     : null;
        String sessionUrl    = test.has("sessionUrl")    ? test.get("sessionUrl").asText()    : null;
        int    healedCount   = test.has("healedCount")   ? test.get("healedCount").asInt()    : 0;
        String aiAnalysis    = test.has("aiAnalysis")    ? test.get("aiAnalysis").asText()    : null;
        String browser     = test.has("browser")      ? capitalize(test.get("browser").asText()) : "";
        String screenshotCell = buildScreenshotCell(test);
        String groupKey    = escapeHtml(groupId);
        int colspan        = showBrowser ? 8 : 7;

        String retryBadge  = retryCount > 0
                ? "<span class=\"retry-badge\">&#x21bb; " + retryCount + "x</span> "
                : "";
        String healedBadge = healedCount > 0
                ? "<span class=\"healed-badge\" title=\"" + healedCount
                  + " locator(s) auto-healed\">&#x26A0; healed</span> "
                : "";

        com.fasterxml.jackson.databind.JsonNode perfNode =
                test.has("performanceMetrics") && !test.get("performanceMetrics").isNull()
                ? test.get("performanceMetrics") : null;

        String stepsHtml   = buildStepTimeline(test);
        boolean hasDetail  = errorMsg != null || stackTrace != null || test.has("failureCategory") || !stepsHtml.isEmpty()
                             || recordingPath != null || aiAnalysis != null || perfNode != null || test.has("testStatus")
                             || test.has("failureDetail");
        String detailRow   = "";
        if (hasDetail) {
            String whyHtml       = buildTestStatusBadge(test)
                    + (test.has("failureCategory") ? buildFailureWhy(test) : "");
            String errorHtml     = whyHtml + (errorMsg != null ? "<div class=\"error-msg\">" + escapeHtml(errorMsg) + "</div>" : "");
            String traceHtml     = stackTrace    != null ? "<pre class=\"stack-trace\">"    + escapeHtml(stackTrace) + "</pre>" : "";
            String recordingHtml = recordingPath != null ? buildRecordingCell(recordingPath) : "";
            String traceLink     = tracePath     != null ? buildTraceLink(tracePath) : "";
            String sessionLink   = sessionUrl    != null ? buildSessionLink(sessionUrl) : "";
            String aiHtml        = aiAnalysis    != null ? buildAiAnalysisPanel(aiAnalysis) : "";
            String perfHtml      = perfNode      != null ? buildPerformancePanel(perfNode) : "";
            String failureDetail = test.has("failureDetail") ? buildFailureDetailPanel(test.get("failureDetail")) : "";
            String stepsSection = !stepsHtml.isEmpty()
                    ? "<div class=\"step-timeline-section\"><div class=\"step-timeline-header\">Steps (" + test.get("steps").size() + ")</div>"
                      + "<div class=\"step-timeline\">" + stepsHtml + "</div></div>"
                    : "";
            String detailDisplay = detailHidden ? " style=\"display:none\"" : "";
            detailRow = "<tr class=\"detail-row group-member\" data-group=\"" + groupKey + "\" id=\"detail-" + rowIndex + "\"" + detailDisplay + ">"
                    + "<td colspan=\"" + colspan + "\"><div class=\"detail-panel\">"
                    + failureDetail + perfHtml + sessionLink + traceLink + stepsSection + recordingHtml + aiHtml + errorHtml + traceHtml
                    + "</div></td>"
                    + "</tr>";
        }

        String rowClass    = "group-member" + (hasDetail ? " expandable" : "");
        String clickAttr   = hasDetail ? " onclick=\"toggleDetail(" + rowIndex + ")\"" : "";
        String memberStyle = collapsed ? " style=\"display:none\"" : "";
        String iconOpen    = (!detailHidden && hasDetail) ? " open" : "";

        StringBuilder tagChips = new StringBuilder();
        StringBuilder tagDataAttr = new StringBuilder();
        if (test.has("tags") && test.get("tags").isArray()) {
            for (JsonNode tagNode : test.get("tags")) {
                String tag = tagNode.asText().trim();
                if (tag.isEmpty()) continue;
                if (tagDataAttr.length() > 0) tagDataAttr.append(",");
                tagDataAttr.append(escapeHtml(tag.toLowerCase()));
                tagChips.append("<span class=\"tag-chip\" data-tag=\"")
                        .append(escapeHtml(tag))
                        .append("\" onclick=\"filterByTag(this.dataset.tag, event)\" title=\"Filter by tag: ")
                        .append(escapeHtml(tag))
                        .append("\">")
                        .append(escapeHtml(tag))
                        .append("</span>");
            }
        }

        String testCellContent;
        if (tagChips.length() > 0) {
            testCellContent = "<div class=\"test-name-cell\">"
                    + "<div class=\"test-name-line\">" + retryBadge + healedBadge + methodName + "</div>"
                    + "<div class=\"tag-chips-wrap\">" + tagChips + "</div>"
                    + "</div>";
        } else {
            testCellContent = retryBadge + healedBadge + methodName;
        }

        String browserCell = showBrowser ? "<td class=\"browser-cell\">" + escapeHtml(browser) + "</td>" : "";

        return "<tr class=\"" + rowClass + "\""
                + clickAttr
                + memberStyle
                + " data-group=\"" + groupKey + "\""
                + " data-status=\"" + status + "\""
                + " data-tags=\"" + tagDataAttr + "\""
                + " data-test=\"" + methodName.toLowerCase() + "\">"
                + "<td class=\"test-id\">" + testCellContent + "</td>"
                + "<td class=\"desc-cell\">" + description + "</td>"
                + browserCell
                + "<td><span class=\"status-badge " + statusClass + "\">" + status + "</span></td>"
                + "<td class=\"numeric\">" + logicMs + "</td>"
                + "<td class=\"numeric\">" + totalMs + "</td>"
                + "<td>" + screenshotCell + "</td>"
                + "<td>" + (hasDetail ? "<span class=\"expand-icon" + iconOpen + "\" id=\"icon-" + rowIndex + "\">&#x25BC;</span>" : "") + "</td>"
                + "</tr>"
                + detailRow;
    }

    private static String buildFailureWhy(JsonNode test) {
        String cat = test.get("failureCategory").asText();
        String label;
        try {
            label = com.seleniumboot.failure.FailureCategory.valueOf(cat).label();
        } catch (IllegalArgumentException e) {
            label = cat;
        }
        String evidence = test.has("failureEvidence") ? test.get("failureEvidence").asText() : "";
        String typeBadge = "";
        if (test.has("failureType")) {
            String type = test.get("failureType").asText();
            String typeLabel;
            try {
                com.seleniumboot.failure.FailureType ft = com.seleniumboot.failure.FailureType.valueOf(type);
                if (ft == com.seleniumboot.failure.FailureType.FLAKY && !label.equalsIgnoreCase("Unknown")) {
                    typeLabel = "Flaky (" + label + ")";
                } else {
                    typeLabel = ft.label();
                }
            } catch (IllegalArgumentException e) {
                typeLabel = type;
            }
            String typeClass = "failure-type-" + type.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
            typeBadge = "<span class=\"failure-type-badge " + typeClass + "\">" + escapeHtml(typeLabel) + "</span>";
        }
        String actionHtml = "";
        if (test.has("suggestedAction") && !test.get("suggestedAction").asText().isBlank()) {
            actionHtml = "<div class=\"failure-action\"><strong>Action:</strong> "
                    + escapeHtml(test.get("suggestedAction").asText()) + "</div>";
        }
        String categoryBadge = typeBadge.contains("(" + escapeHtml(label) + ")")
                ? ""
                : "<span class=\"failure-badge\">" + escapeHtml(label) + "</span>";
        return "<div class=\"failure-why\">" + typeBadge + categoryBadge
                + escapeHtml(evidence) + "</div>" + actionHtml;
    }

    private static String buildFailureDetailPanel(JsonNode detail) {
        if (detail == null || detail.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<div class=\"failure-detail-section\">"
                + "<div class=\"step-timeline-header\">Failure detail</div><ul>");
        detailItem(sb, "Failure type", detail, "failureType");
        detailItem(sb, "Suggested action", detail, "suggestedAction");
        detailItem(sb, "Failing locator", detail, "failingLocator");
        detailItem(sb, "Application frame", detail, "applicationFrame");
        detailItem(sb, "Last successful step", detail, "lastSuccessfulStep");
        detailItem(sb, "Page URL", detail, "pageUrl");
        detailItem(sb, "Page title", detail, "pageTitle");
        detailItem(sb, "Test status", detail, "testStatus");
        sb.append("</ul>");

        if (detail.has("healEvents") && detail.get("healEvents").size() > 0) {
            sb.append("<div class=\"step-timeline-header\">Self-healing events</div><ul>");
            for (JsonNode event : detail.get("healEvents")) {
                sb.append("<li>").append(escapeHtml(safeStr(event, "originalLocator")))
                  .append(" &rarr; ").append(escapeHtml(safeStr(event, "healedLocator")));
                if (event.has("strategy")) {
                    sb.append(" (").append(escapeHtml(event.get("strategy").asText())).append(")");
                }
                sb.append("</li>");
            }
            sb.append("</ul>");
        }

        if (detail.has("consoleErrors") && detail.get("consoleErrors").size() > 0) {
            sb.append("<div class=\"step-timeline-header\">Console errors</div><ul>");
            for (JsonNode error : detail.get("consoleErrors")) {
                sb.append("<li>").append(escapeHtml(error.asText())).append("</li>");
            }
            sb.append("</ul>");
        }

        if (detail.has("groupContext")) {
            JsonNode group = detail.get("groupContext");
            sb.append("<div class=\"step-timeline-header\">Same-cause group</div><ul>");
            if (group.has("size")) sb.append("<li>Size: ").append(group.get("size").asInt()).append("</li>");
            detailItem(sb, "Representative", group, "representativeTest");
            sb.append("</ul>");
            if (group.has("representativeScreenshot")) {
                com.fasterxml.jackson.databind.node.ObjectNode screenshot =
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                screenshot.put("screenshotPath", group.get("representativeScreenshot").asText());
                sb.append(buildScreenshotCell(screenshot));
            }
            if (group.has("representativeTrace")) {
                sb.append(buildTraceLink(group.get("representativeTrace").asText()));
            }
        }

        return sb.append("</div>").toString();
    }

    private static void detailItem(StringBuilder html, String label, JsonNode node, String field) {
        if (!node.has(field)) return;
        html.append("<li><strong>").append(label).append(":</strong> ")
            .append(escapeHtml(node.get(field).asText())).append("</li>");
    }

    private static String testStatusOf(JsonNode test) {
        return test.has("testStatus") ? test.get("testStatus").asText() : "";
    }

    private static String testStatusLabel(String status) {
        try {
            return com.seleniumboot.flakiness.TestStatus.valueOf(status).label();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    private static String buildTestStatusBadge(JsonNode test) {
        String status = testStatusOf(test);
        if (status.isEmpty()) return "";
        return "<div class=\"failure-why\"><span class=\"failure-badge\">"
                + escapeHtml(testStatusLabel(status)) + "</span></div>";
    }

    /** Tests flagged by retries or history, next to the Flakiness Radar. Empty when none are. */
    private static String buildTestStatusSection(JsonNode tests) {
        if (tests == null) return "";
        java.util.List<JsonNode> flagged = new java.util.ArrayList<>();
        for (JsonNode t : tests) {
            if (!testStatusOf(t).isEmpty()) flagged.add(t);
        }
        if (flagged.isEmpty()) return "";
        String[] order = {"NEWLY_FAILING", "CONSISTENTLY_FAILING", "INTERMITTENT", "PASSED_ON_RETRY"};
        flagged.sort(java.util.Comparator.comparingInt(t -> java.util.Arrays.asList(order).indexOf(testStatusOf(t))));

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card section-mb\">\n");
        sb.append("  <div class=\"card-header\">&#x26A0; Test status (this run vs history)</div>\n");
        sb.append("  <div class=\"card-body flakiness-section\">\n");
        for (JsonNode t : flagged) {
            String status = testStatusOf(t);
            String cls = "NEWLY_FAILING".equals(status) || "CONSISTENTLY_FAILING".equals(status)
                    ? "risk-high" : "risk-watch";
            String id = t.has("testId") ? t.get("testId").asText() : "";
            int lastDot = id.lastIndexOf('.');
            String shortName = lastDot >= 0 ? id.substring(lastDot + 1) : id;
            sb.append("    <div class=\"flakiness-item\">\n");
            sb.append("      <span class=\"").append(cls).append("\">")
              .append(escapeHtml(testStatusLabel(status))).append("</span>\n");
            sb.append("      <span class=\"flakiness-name\" title=\"").append(escapeHtml(id)).append("\">")
              .append(escapeHtml(shortName)).append("</span>\n");
            sb.append("    </div>\n");
        }
        sb.append("  </div>\n</div>\n");
        return sb.toString();
    }

    private static String buildFailureRows(JsonNode tests) {
        return buildFailureRows(tests, false);
    }

    private static String buildFailureRows(JsonNode tests, boolean showBrowser) {
        if (tests == null) return noFailuresRow();
        // Newly failing first: it is the one a recent change most likely broke. The sort is
        // stable, so the classes and tests keep their order otherwise.
        java.util.List<JsonNode> ordered = new java.util.ArrayList<>();
        tests.forEach(ordered::add);
        ordered.sort(java.util.Comparator.comparingInt(t -> "NEWLY_FAILING".equals(testStatusOf(t)) ? 0 : 1));
        java.util.Map<String, java.util.List<JsonNode>> byClass = new java.util.LinkedHashMap<>();
        for (JsonNode test : ordered) {
            if (!"FAILED".equals(test.has("status") ? test.get("status").asText() : "")) continue;
            String cls = test.has("testClassName") ? test.get("testClassName").asText() : "";
            if (cls.isEmpty()) cls = "Unknown";
            String groupKey = showBrowser
                    ? cls + " [" + capitalize(test.has("browser") ? test.get("browser").asText("") : "") + "]"
                    : cls;
            byClass.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(test);
        }
        if (byClass.isEmpty()) return noFailuresRow();

        int colspan = showBrowser ? 8 : 7;
        StringBuilder rows = new StringBuilder();
        int rowIndex = 50000; // offset avoids ID conflicts with the test-cases table
        for (java.util.Map.Entry<String, java.util.List<JsonNode>> entry : byClass.entrySet()) {
            String groupKey = escapeHtml(entry.getKey());
            int total = entry.getValue().size();
            // Non-collapsible group header for failures (always expanded)
            rows.append("<tr class=\"group-header\">"
                    + "<td colspan=\"" + colspan + "\">"
                    + "<span class=\"group-icon\">&#x25BC;</span> "
                    + "<strong>" + groupKey + "</strong>"
                    + "<span class=\"group-count\"> &mdash; " + total + " failure" + (total != 1 ? "s" : "") + "</span>"
                    + "</td></tr>");
            for (JsonNode test : entry.getValue()) {
                // The "failures:" prefix keeps these rows out of the Test Cases tab's group toggle,
                // which selects by data-group. Details start collapsed; the row stays visible.
                rows.append(buildRow(test, rowIndex++, "failures:" + entry.getKey(), false, true, showBrowser));
            }
        }
        return rows.toString();
    }

    /**
     * "N tests, same cause" panel shown above the flat failure list. Only causes shared by two or
     * more tests are listed, largest first; the flat list below still shows every failure.
     */
    private static String buildFailureGroups(JsonNode tests) {
        if (tests == null) return "";
        java.util.Map<String, java.util.List<JsonNode>> bySignature = new java.util.LinkedHashMap<>();
        for (JsonNode test : tests) {
            if (!"FAILED".equals(test.has("status") ? test.get("status").asText() : "")) continue;
            String signature = test.has("failureSignature")
                    ? test.get("failureSignature").asText()
                    : com.seleniumboot.failure.FailureSignature.of(
                            test.has("errorMessage") ? test.get("errorMessage").asText() : null,
                            test.has("stackTrace") ? test.get("stackTrace").asText() : null);
            bySignature.computeIfAbsent(signature, k -> new java.util.ArrayList<>()).add(test);
        }
        java.util.List<java.util.List<JsonNode>> groups = new java.util.ArrayList<>();
        for (java.util.List<JsonNode> g : bySignature.values()) {
            if (g.size() >= 2) groups.add(g);
        }
        if (groups.isEmpty()) return "";
        groups.sort((a, b) -> Integer.compare(b.size(), a.size()));

        StringBuilder sb = new StringBuilder("<div class=\"card section-mb failure-groups\">")
                .append("<div class=\"card-header\">Same cause (")
                .append(groups.size()).append(groups.size() == 1 ? " group" : " groups").append(")</div>")
                .append("<div class=\"card-body\">");
        for (java.util.List<JsonNode> g : groups) {
            JsonNode first = g.get(0);
            sb.append("<div class=\"failure-group\"><div class=\"failure-group-title\">");
            if (first.has("failureCategory")) {
                sb.append(buildFailureWhy(first)).append("");
            }
            sb.append("<strong>").append(g.size()).append(" tests, same cause</strong></div>");
            String msg = first.has("errorMessage") ? first.get("errorMessage").asText() : "";
            int nl = msg.indexOf('\n');
            if (nl >= 0) msg = msg.substring(0, nl);
            if (!msg.isEmpty()) {
                sb.append("<div class=\"failure-group-msg\">").append(escapeHtml(msg)).append("</div>");
            }
            sb.append("<ul class=\"failure-group-tests\">");
            for (JsonNode t : g) {
                sb.append("<li>").append(escapeHtml(t.has("testId") ? t.get("testId").asText() : "")).append("</li>");
            }
            sb.append("</ul></div>");
        }
        return sb.append("</div></div>").toString();
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

    /** Minimum bar width in percent, so very short tests stay visible. */
    private static final double MIN_TIMELINE_BAR_PCT = 0.4;

    private static final class TimelineEntry {
        String testId;
        String shortName;
        String thread;
        String status;
        long start;
        long end;
        long duration;
    }

    /**
     * Gantt-style timeline: one lane per thread, one bar per test positioned by
     * {@code startedAtMs} offset from the earliest start. {@code end = startedAtMs + totalMs}.
     * Tests without {@code startedAtMs} are left out; the card is hidden when none have it (e.g. old history files).
     * Handles single-thread runs and degrades cleanly.
     */
    static String buildTimelineSection(JsonNode tests) {
        if (tests == null || !tests.isArray() || tests.size() == 0) return "";

        java.util.List<TimelineEntry> entries = new java.util.ArrayList<>();
        for (JsonNode test : tests) {
            // Tests that never started (e.g. skipped by dependency) have no start time; leave them out
            long startedAt = test.hasNonNull("startedAtMs") ? test.get("startedAtMs").asLong() : 0L;
            if (startedAt <= 0) continue;
            long totalMs = test.has("totalMs") ? test.get("totalMs").asLong() : 0L;
            if (totalMs <= 0) continue;

            TimelineEntry e = new TimelineEntry();
            String rawId = test.has("testId") ? test.get("testId").asText() : "unknown";
            int dot = rawId.lastIndexOf('.');
            e.testId    = rawId;
            e.shortName = dot >= 0 ? rawId.substring(dot + 1) : rawId;
            String thread = test.hasNonNull("thread") ? test.get("thread").asText("") : "";
            e.thread = thread.isBlank() ? "unknown" : thread;
            e.status = test.has("status") ? test.get("status").asText() : "UNKNOWN";
            e.duration = totalMs;
            e.start = startedAt;
            e.end = startedAt + totalMs;
            entries.add(e);
        }

        if (entries.isEmpty()) return "";

        // Group by thread name; one lane per thread
        java.util.Map<String, java.util.List<TimelineEntry>> lanes =
                new java.util.LinkedHashMap<>();
        for (TimelineEntry e : entries) {
            lanes.computeIfAbsent(e.thread, k -> new java.util.ArrayList<>()).add(e);
        }
        for (java.util.List<TimelineEntry> lane : lanes.values()) {
            lane.sort(java.util.Comparator.comparingLong(e -> e.start));
        }

        long t0 = Long.MAX_VALUE;
        long t1 = Long.MIN_VALUE;
        for (TimelineEntry e : entries) {
            t0 = Math.min(t0, e.start);
            t1 = Math.max(t1, e.end);
        }
        long span = Math.max(1L, t1 - t0);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card section-mb timeline-card\">\n");
        sb.append("  <div class=\"card-header timeline-header\"><span>Timeline</span>")
          .append("<span class=\"timeline-legend\">")
          .append("<span class=\"tl-key tl-key-passed\">Passed</span>")
          .append("<span class=\"tl-key tl-key-failed\">Failed</span>")
          .append("<span class=\"tl-key tl-key-skipped\">Skipped</span>")
          .append("</span></div>\n");
        sb.append("  <div class=\"card-body timeline-body\">\n");
        sb.append("    <div class=\"gantt-wrap\"><div class=\"gantt\">\n");

        // Time axis (0..100% of the run span)
        sb.append("      <div class=\"gantt-row gantt-axis-row\">")
          .append("<div class=\"gantt-lane-name\"></div>")
          .append("<div class=\"gantt-track gantt-axis-track\">");
        for (int i = 0; i <= 4; i++) {
            long offset = Math.round(span * i / 4.0);
            sb.append("<span class=\"gantt-tick\" style=\"left:").append(timelinePct(i * 25.0)).append("%\">")
              .append(escapeHtml(formatTimelineTick(offset)))
              .append("</span>");
        }
        sb.append("</div></div>\n");

        // One lane per thread
        for (java.util.Map.Entry<String, java.util.List<TimelineEntry>> lane : lanes.entrySet()) {
            sb.append("      <div class=\"gantt-row gantt-lane\">")
              .append("<div class=\"gantt-lane-name\" title=\"").append(escapeHtml(lane.getKey())).append("\">")
              .append(escapeHtml(timelineLaneLabel(lane.getKey()))).append("</div>")
              .append("<div class=\"gantt-track\">");

            for (TimelineEntry e : lane.getValue()) {
                double leftPct  = (e.start - t0) * 100.0 / span;
                double widthPct = e.duration * 100.0 / span;
                if (widthPct < MIN_TIMELINE_BAR_PCT) widthPct = MIN_TIMELINE_BAR_PCT;
                if (leftPct < 0) leftPct = 0;
                if (leftPct + widthPct > 100) leftPct = Math.max(0, 100 - widthPct);

                String title = escapeHtml(e.testId)
                        + "&#10;Thread: " + escapeHtml(e.thread)
                        + "&#10;Status: " + escapeHtml(e.status)
                        + "&#10;Duration: " + formatTimelineDuration(e.duration)
                        + "&#10;Started: +" + formatTimelineDuration(Math.max(0, e.start - t0));

                sb.append("<div class=\"gantt-bar gantt-bar-").append(timelineStatusSlug(e.status))
                  .append("\" style=\"left:").append(timelinePct(leftPct))
                  .append("%;width:").append(timelinePct(widthPct))
                  .append("%\" title=\"").append(title).append("\">")
                  .append("<span class=\"gantt-bar-label\">").append(escapeHtml(e.shortName)).append("</span>")
                  .append("</div>");
            }
            sb.append("</div></div>\n");
        }

        sb.append("    </div></div>\n");
        sb.append("  </div>\n</div>\n");
        return sb.toString();
    }

    /** Drops TestNG's shared "TestNG-test-" thread prefix so the distinguishing part of the name fits the lane. */
    static String timelineLaneLabel(String thread) {
        return thread.startsWith("TestNG-test-") && thread.length() > "TestNG-test-".length()
                ? thread.substring("TestNG-test-".length()) : thread;
    }

    private static String timelineStatusSlug(String status) {
        if (status == null) return "unknown";
        switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "PASSED":  return "passed";
            case "FAILED":  return "failed";
            case "SKIPPED": return "skipped";
            default:        return "unknown";
        }
    }

    private static String timelinePct(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", Math.max(0, value));
    }

    private static String formatTimelineTick(long ms) {
        if (ms <= 0) return "0s";
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format(java.util.Locale.ROOT, "%.1fs", ms / 1000.0);
        long totalSeconds = ms / 1000;
        return (totalSeconds / 60) + "m "
                + String.format(java.util.Locale.ROOT, "%02d", totalSeconds % 60) + "s";
    }

    private static String formatTimelineDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        if (ms < 60_000) return String.format(java.util.Locale.ROOT, "%.1f s", ms / 1000.0);
        long totalSeconds = ms / 1000;
        return (totalSeconds / 60) + "m "
                + String.format(java.util.Locale.ROOT, "%02d", totalSeconds % 60) + "s";
    }

    private static String buildRecordingCell(String recordingPath) {
        File gif = new File(recordingPath);
        String label = gif.getName();
        if (!gif.exists()) {
            return "<div class=\"recording-section\"><span class=\"recording-label\">&#x1F3A5; Recording:</span> "
                    + "<span class=\"recording-missing\">" + escapeHtml(label) + " (file not found)</span></div>";
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(gif.toPath());
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            // Only embed if under 3 MB; otherwise show a path hint
            if (bytes.length < 3 * 1024 * 1024) {
                return "<div class=\"recording-section\">"
                        + "<div class=\"recording-label\">&#x1F3A5; Recording</div>"
                        + "<img src=\"data:image/gif;base64," + base64 + "\" class=\"recording-gif\" />"
                        + "</div>";
            }
        } catch (java.io.IOException ignored) {}
        // Fallback: show path
        return "<div class=\"recording-section\"><span class=\"recording-label\">&#x1F3A5; Recording:</span> "
                + "<span class=\"recording-path\">" + escapeHtml(recordingPath) + "</span></div>";
    }

    private static String buildPerformancePanel(com.fasterxml.jackson.databind.JsonNode perf) {
        double lcp     = perf.has("lcp")      ? perf.get("lcp").asDouble(-1)      : -1;
        double fcp     = perf.has("fcp")      ? perf.get("fcp").asDouble(-1)      : -1;
        double ttfb    = perf.has("ttfb")     ? perf.get("ttfb").asDouble(-1)     : -1;
        double cls     = perf.has("cls")      ? perf.get("cls").asDouble(-1)      : -1;
        double domLoad = perf.has("domLoad")  ? perf.get("domLoad").asDouble(-1)  : -1;
        double pgLoad  = perf.has("pageLoad") ? perf.get("pageLoad").asDouble(-1) : -1;

        // Return nothing if all metrics are unavailable
        if (lcp < 0 && fcp < 0 && ttfb < 0 && cls < 0 && domLoad < 0 && pgLoad < 0) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"perf-metrics\"><span class=\"perf-label\">&#x26A1; Performance</span>");
        if (lcp     >= 0) sb.append(perfChip("LCP",     formatMs(lcp),    lcpClass(lcp)));
        if (fcp     >= 0) sb.append(perfChip("FCP",     formatMs(fcp),    fcpClass(fcp)));
        if (ttfb    >= 0) sb.append(perfChip("TTFB",    formatMs(ttfb),   ttfbClass(ttfb)));
        if (cls     >= 0) sb.append(perfChip("CLS",     String.format("%.3f", cls), clsClass(cls)));
        if (domLoad >= 0) sb.append(perfChip("DOM",     formatMs(domLoad), "perf-na"));
        if (pgLoad  >= 0) sb.append(perfChip("Load",    formatMs(pgLoad),  "perf-na"));
        sb.append("</div>");
        return sb.toString();
    }

    private static String perfChip(String label, String value, String cls) {
        return "<span class=\"perf-chip " + cls + "\">" + label + " " + value + "</span>";
    }

    private static String formatMs(double ms) {
        return ms >= 1000
            ? String.format("%.2fs", ms / 1000)
            : String.format("%.0fms", ms);
    }

    private static String lcpClass(double ms) {
        return ms < 2500 ? "perf-good" : ms < 4000 ? "perf-warn" : "perf-bad";
    }

    private static String fcpClass(double ms) {
        return ms < 1800 ? "perf-good" : ms < 3000 ? "perf-warn" : "perf-bad";
    }

    private static String ttfbClass(double ms) {
        return ms < 800 ? "perf-good" : ms < 1800 ? "perf-warn" : "perf-bad";
    }

    private static String clsClass(double score) {
        return score < 0.1 ? "perf-good" : score < 0.25 ? "perf-warn" : "perf-bad";
    }

    private static String buildAiAnalysisPanel(String analysis) {
        return "<div class=\"ai-analysis-section\">"
                + "<div class=\"ai-analysis-header\">&#x1F916; AI Failure Analysis</div>"
                + "<div class=\"ai-analysis-body\">" + escapeHtml(analysis) + "</div>"
                + "</div>";
    }

    private static String buildFlakinessSection() {
        java.util.List<com.seleniumboot.flakiness.FlakinessScore> scores =
                com.seleniumboot.flakiness.FlakinessAnalyzer.getLastResult();
        java.util.List<com.seleniumboot.flakiness.FlakinessScore> risky = scores.stream()
                .filter(s -> s.getRisk() != com.seleniumboot.flakiness.FlakinessScore.Risk.STABLE)
                .collect(java.util.stream.Collectors.toList());
        if (risky.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card section-mb\">\n");
        sb.append("  <div class=\"card-header\">&#x26A0; Flakiness Radar</div>\n");
        sb.append("  <div class=\"card-body flakiness-section\">\n");
        for (com.seleniumboot.flakiness.FlakinessScore s : risky) {
            String riskClass = s.getRisk() == com.seleniumboot.flakiness.FlakinessScore.Risk.HIGH
                    ? "risk-high" : "risk-watch";
            String riskLabel = s.getRisk().name();
            int lastDot = s.getTestId().lastIndexOf('.');
            String shortName = lastDot >= 0 ? s.getTestId().substring(lastDot + 1) : s.getTestId();
            sb.append("    <div class=\"flakiness-item\">\n");
            sb.append("      <span class=\"").append(riskClass).append("\">").append(riskLabel).append("</span>\n");
            sb.append("      <span class=\"flakiness-name\" title=\"").append(escapeHtml(s.getTestId())).append("\">")
              .append(escapeHtml(shortName)).append("</span>\n");
            sb.append("      <span class=\"flakiness-rate\">")
              .append(String.format("%.1f", s.getFailureRate())).append("% fail")
              .append(" (").append(s.getRunsAnalysed()).append(" runs)")
              .append("</span>\n");
            sb.append("    </div>\n");
        }
        sb.append("  </div>\n</div>\n");
        return sb.toString();
    }

    private static String buildSessionLink(String url) {
        return "<div class=\"trace-link-section\">"
                + "<a href=\"" + escapeHtml(url) + "\" target=\"_blank\" class=\"trace-link\">"
                + "&#x2601; View Session</a></div>";
    }

    private static String buildTraceLink(String tracePath) {
        // tracePath is relative to target/ — the report is also at target/, so the href is correct
        return "<div class=\"trace-link-section\">"
                + "<a href=\"" + escapeHtml(tracePath) + "\" target=\"_blank\" class=\"trace-link\">"
                + "&#x1F50D; View Trace</a></div>";
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


    private static class TagStat {
        int total = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;
    }

    private static String buildTagResultsSection(JsonNode tests) {
        if (tests == null) return "";
        java.util.Map<String, TagStat> statsMap = new java.util.LinkedHashMap<>();
        for (JsonNode test : tests) {
            if (!test.has("tags") || !test.get("tags").isArray()) continue;
            String status = test.has("status") ? test.get("status").asText() : "UNKNOWN";
            for (JsonNode tagNode : test.get("tags")) {
                String tag = tagNode.asText().trim();
                if (tag.isEmpty()) continue;
                TagStat stat = statsMap.computeIfAbsent(tag, k -> new TagStat());
                stat.total++;
                if ("PASSED".equalsIgnoreCase(status)) stat.passed++;
                else if ("FAILED".equalsIgnoreCase(status)) stat.failed++;
                else if ("SKIPPED".equalsIgnoreCase(status)) stat.skipped++;
            }
        }
        if (statsMap.isEmpty()) return "";

        java.util.List<String> sortedTags = new java.util.ArrayList<>(statsMap.keySet());
        sortedTags.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card section-mb\" id=\"tagResultsCard\">\n")
          .append("  <div class=\"card-header\" style=\"display:flex;justify-content:space-between;align-items:center;\">\n")
          .append("    <span>Results by Tag</span>\n")
          .append("    <span class=\"group-count\">").append(sortedTags.size())
          .append(" tag").append(sortedTags.size() != 1 ? "s" : "").append("</span>\n")
          .append("  </div>\n")
          .append("  <div class=\"card-body\" style=\"padding:16px 24px;\">\n")
          .append("    <div class=\"table-wrap\">\n")
          .append("      <table class=\"tag-summary-table\">\n")
          .append("        <thead>\n")
          .append("          <tr>\n")
          .append("            <th>Tag</th>\n")
          .append("            <th>Total</th>\n")
          .append("            <th>Passed</th>\n")
          .append("            <th>Failed</th>\n")
          .append("            <th>Skipped</th>\n")
          .append("            <th>Pass Rate</th>\n")
          .append("          </tr>\n")
          .append("        </thead>\n")
          .append("        <tbody>\n");

        for (String tag : sortedTags) {
            TagStat stat = statsMap.get(tag);
            double rate = stat.total == 0 ? 0.0 : Math.round((stat.passed * 1000.0) / stat.total) / 10.0;
            String rateClass = rate >= 80.0 ? "rate-good" : rate >= 60.0 ? "rate-warn" : "rate-bad";
            String escapedTag = escapeHtml(tag);

            String passedBadge = stat.passed > 0
                    ? "<span class=\"status-badge status-passed\">" + stat.passed + " passed</span>"
                    : "<span style=\"color:var(--text-secondary);\">0</span>";
            String failedBadge = stat.failed > 0
                    ? "<span class=\"status-badge status-failed\">" + stat.failed + " failed</span>"
                    : "<span style=\"color:var(--text-secondary);\">0</span>";
            String skippedBadge = stat.skipped > 0
                    ? "<span class=\"status-badge status-skipped\">" + stat.skipped + " skipped</span>"
                    : "<span style=\"color:var(--text-secondary);\">0</span>";

            sb.append("          <tr>\n")
              .append("            <td>\n")
              .append("              <span class=\"tag-chip\" data-tag=\"")
              .append(escapedTag)
              .append("\" onclick=\"filterByTag(this.dataset.tag, event)\" title=\"Filter by tag: ")
              .append(escapedTag)
              .append("\">")
              .append(escapedTag)
              .append("</span>\n")
              .append("            </td>\n")
              .append("            <td><strong>").append(stat.total).append("</strong></td>\n")
              .append("            <td>").append(passedBadge).append("</td>\n")
              .append("            <td>").append(failedBadge).append("</td>\n")
              .append("            <td>").append(skippedBadge).append("</td>\n")
              .append("            <td><span class=\"tag-rate ").append(rateClass).append("\">")
              .append(String.format(java.util.Locale.US, "%.1f%%", rate))
              .append("</span></td>\n")
              .append("          </tr>\n");
        }

        sb.append("        </tbody>\n")
          .append("      </table>\n")
          .append("    </div>\n")
          .append("  </div>\n")
          .append("</div>\n");

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
        return loadResource("report-template.html");
    }

    private static String loadMarkup() {
        return loadResource("report/partials/appbar.html")
                + loadResource("report/partials/lightbox.html")
                + "<div class=\"layout\">\n"
                + loadResource("report/partials/sidebar.html")
                + "<main class=\"main-content\">\n"
                + loadResource("report/partials/dashboard.html")
                + loadResource("report/partials/tests.html")
                + loadResource("report/partials/failures.html")
                + "</main>\n</div>\n";
    }

    private static String loadResource(String name) {
        try (InputStream is = HtmlReportGenerator.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            if (is == null) {
                throw new IllegalStateException(name + " not found in classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to load " + name, e);
        }
    }
}
