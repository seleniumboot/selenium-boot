package com.seleniumboot.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.io.File;
import java.io.FileWriter;
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

        String htmlTemplate = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Selenium Boot Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Roboto', sans-serif; background: #f5f5f5; color: #212121; }

        .app-bar {
            background: #303f9f;
            color: #fff;
            padding: 24px 32px;
            box-shadow: 0 2px 4px rgba(0,0,0,.14), 0 3px 4px rgba(0,0,0,.12), 0 1px 5px rgba(0,0,0,.2);
            position: sticky; top: 0; z-index: 10;
        }
        .app-bar h1 { font-size: 20px; font-weight: 500; letter-spacing: .5px; }
        .app-bar p  { font-size: 14px; font-weight: 300; opacity: .85; margin-top: 4px; }

        .content { max-width: 1200px; margin: 0 auto; padding: 24px 16px 48px; }

        .card {
            background: #fff; border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,.12), 0 1px 2px rgba(0,0,0,.06);
            transition: box-shadow .2s, transform .2s;
        }
        .card:hover {
            box-shadow: 0 4px 6px rgba(0,0,0,.12), 0 2px 4px rgba(0,0,0,.08);
            transform: translateY(-2px);
        }
        .card-header { padding: 16px 24px; font-size: 16px; font-weight: 500; color: #424242; border-bottom: 1px solid #e0e0e0; }
        .card-body   { padding: 24px; }

        .summary-row { display: flex; gap: 16px; margin-bottom: 24px; }
        .stat-card   { flex: 1; text-align: center; padding: 24px 16px; }
        .stat-value  { font-size: 36px; font-weight: 700; color: #303f9f; }
        .stat-label  { font-size: 13px; font-weight: 400; color: #757575; margin-top: 8px; text-transform: uppercase; letter-spacing: .5px; }

        .charts-row { display: flex; gap: 16px; margin-bottom: 24px; }
        .chart-card { flex: 1; }
        .chart-card canvas { width: 100%% !important; height: 280px !important; }

        .table-card { margin-bottom: 24px; overflow: hidden; }
        table { width: 100%%; border-collapse: collapse; }
        thead th {
            background: #303f9f; color: #fff;
            padding: 12px 16px; font-size: 13px; font-weight: 500;
            text-transform: uppercase; letter-spacing: .5px; text-align: left;
        }
        tbody td { padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #e0e0e0; }
        tbody tr:nth-child(even) { background: #fafafa; }
        tbody tr:hover { background: #e8eaf6; }
        td.test-id { font-family: 'Roboto Mono', monospace; font-size: 13px; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        td.numeric { text-align: right; font-variant-numeric: tabular-nums; }

        .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
            gap: 16px;
        }
        .meta-item {
            display: flex; flex-direction: column;
            padding: 12px 16px;
            background: #f5f5f5; border-radius: 6px;
            border-left: 3px solid #303f9f;
        }
        .meta-label {
            font-size: 11px; font-weight: 500; color: #757575;
            text-transform: uppercase; letter-spacing: .5px; margin-bottom: 4px;
        }
        .meta-value {
            font-size: 14px; font-weight: 500; color: #212121;
            word-break: break-all;
        }

        .passed-card  .stat-value { color: #2e7d32; }
        .failed-card  .stat-value { color: #c62828; }
        .skipped-card .stat-value { color: #e65100; }

        .status-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
        .status-passed  { background: #e8f5e9; color: #2e7d32; }
        .status-failed  { background: #ffebee; color: #c62828; }
        .status-skipped { background: #fff3e0; color: #e65100; }
        .status-unknown { background: #f5f5f5; color: #757575; }

        .footer { text-align: center; padding: 16px; font-size: 12px; color: #9e9e9e; }

        @media (max-width: 768px) {
            .summary-row, .charts-row { flex-direction: column; }
            .app-bar { padding: 16px; }
            .content  { padding: 16px 8px; }
        }
    </style>
</head>
<body>

<div class="app-bar">
    <h1>Selenium Boot Report</h1>
    <p>Execution Metrics &amp; Performance Analysis</p>
</div>

<div class="content">
    %s
    <div class="summary-row">
        <div class="card stat-card passed-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Passed</div>
        </div>
        <div class="card stat-card failed-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Failed</div>
        </div>
        <div class="card stat-card skipped-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Skipped</div>
        </div>
    </div>
    <div class="summary-row">
        <div class="card stat-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Total Tests</div>
        </div>
        <div class="card stat-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Total Time (ms)</div>
        </div>
        <div class="card stat-card">
            <div class="stat-value">%d</div>
            <div class="stat-label">Avg Time (ms)</div>
        </div>
    </div>

    <div class="charts-row">
        <div class="card chart-card">
            <div class="card-header">Execution Time Percentiles</div>
            <div class="card-body"><canvas id="executionChart"></canvas></div>
        </div>
        <div class="card chart-card">
            <div class="card-header">Driver Startup Percentiles</div>
            <div class="card-body"><canvas id="driverChart"></canvas></div>
        </div>
    </div>

    <div class="card table-card">
        <div class="card-header">Test Details</div>
        <table>
            <thead>
                <tr>
                    <th>Test</th>
                    <th>Thread</th>
                    <th>Status</th>
                    <th>Driver Startup (ms)</th>
                    <th>Test Logic (ms)</th>
                    <th>Total (ms)</th>
                    <th>Screenshot</th>
                </tr>
            </thead>
            <tbody>
                %s
            </tbody>
        </table>
    </div>

    <div class="footer">Report generated by Selenium Boot</div>

</div>

<script>

const executionData = JSON.parse('%s');
const driverData = JSON.parse('%s');

function safeValue(obj, key) {
    return obj && obj[key] ? obj[key] : 0;
}

function createChart(canvasId, label, dataObj, bgColor, borderColor) {

    const ctx = document.getElementById(canvasId);
    if (!ctx) return;

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['P50', 'P90', 'P95', 'P99'],
            datasets: [{
                label: label,
                data: [
                    safeValue(dataObj, 'p50'),
                    safeValue(dataObj, 'p90'),
                    safeValue(dataObj, 'p95'),
                    safeValue(dataObj, 'p99')
                ],
                backgroundColor: bgColor,
                borderColor: borderColor,
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { labels: { font: { family: 'Roboto', size: 13 } } }
            },
            scales: {
                x: { ticks: { font: { family: 'Roboto' } }, grid: { display: false } },
                y: { ticks: { font: { family: 'Roboto' } }, grid: { color: '#e0e0e0' }, beginAtZero: true }
            }
        }
    });
}

createChart('executionChart', 'Execution Time (ms)', executionData, 'rgba(48,63,159,.25)', '#303f9f');
createChart('driverChart', 'Driver Startup (ms)', driverData, 'rgba(0,137,123,.25)', '#00897b');

</script>

</body>
</html>
""";

        return String.format(htmlTemplate,
                metadataSection,
                passedTests,
                failedTests,
                skippedTests,
                totalTests,
                totalTimeMs,
                averageTimeMs,
                rows.toString(),
                executionPercentiles.replace("'", "\\'"),
                driverPercentiles.replace("'", "\\'")
        );
    }
}
