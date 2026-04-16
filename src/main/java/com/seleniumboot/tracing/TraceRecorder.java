package com.seleniumboot.tracing;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.reporting.ScreenshotManager;
import com.seleniumboot.steps.StepRecord;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates a self-contained HTML trace file per failed (or optionally passing) test.
 *
 * <p>The trace file captures:
 * <ul>
 *   <li>Test metadata — name, class, browser, duration, timestamp</li>
 *   <li>Named step timeline — each step logged via {@code StepLogger} as a clickable block</li>
 *   <li>Per-step screenshots — click a step to reveal its screenshot</li>
 *   <li>Final-state screenshot — taken at the moment of failure</li>
 *   <li>Error message + full stack trace</li>
 * </ul>
 *
 * <p>Output: {@code target/traces/{ClassName}/{testMethod}-trace.html}
 *
 * <p>Activated automatically by {@code TestExecutionListener} when
 * {@code tracing.enabled: true} is set in {@code selenium-boot.yml}.
 *
 * <pre>
 * # selenium-boot.yml
 * tracing:
 *   enabled: true
 *   captureOnPass: false  # set true to generate traces for passing tests too
 * </pre>
 */
@SeleniumBootApi(since = "1.7.0")
public final class TraceRecorder {

    private static final Logger LOG = Logger.getLogger(TraceRecorder.class.getName());

    private TraceRecorder() {}

    // ------------------------------------------------------------------
    // Public API — called by TestExecutionListener
    // ------------------------------------------------------------------

    /**
     * Generates a trace HTML file for the given test and records its path in
     * {@link ExecutionMetrics} so that the HTML report can link to it.
     *
     * @param testId    fully-qualified test method name (the ExecutionMetrics key)
     * @param testName  short test method name (used as the file name)
     */
    public static void save(String testId, String testName) {
        TestTiming timing = ExecutionMetrics.getTiming(testId);
        if (timing == null) {
            LOG.warning("[TraceRecorder] No timing data for test: " + testId);
            return;
        }

        String className = timing.getTestClassName() != null ? timing.getTestClassName() : "UnknownClass";
        String finalScreenshot = captureFinalScreenshot(testName);

        File outFile = traceFile(className, testName);
        outFile.getParentFile().mkdirs();

        String html = buildHtml(timing, testName, finalScreenshot);

        try (FileWriter fw = new FileWriter(outFile)) {
            fw.write(html);
        } catch (IOException e) {
            LOG.warning("[TraceRecorder] Failed to write trace file: " + e.getMessage());
            return;
        }

        // Store relative path from target/ so HtmlReportGenerator can link to it
        String relativePath = "traces/" + className + "/" + sanitize(testName) + "-trace.html";
        ExecutionMetrics.recordTracePath(testId, relativePath);
        LOG.info("[TraceRecorder] Trace saved: " + outFile.getAbsolutePath());
    }

    // ------------------------------------------------------------------
    // HTML generation
    // ------------------------------------------------------------------

    public static String buildHtml(TestTiming timing, String testName, String finalScreenshotBase64) {
        List<StepRecord> steps = timing.getSteps();
        String className   = nvl(timing.getTestClassName(), "Unknown");
        String browser     = nvl(timing.getBrowser(), "—");
        String durationStr = timing.getTotalTime() > 0 ? timing.getTotalTime() + " ms" : "—";
        String timestamp   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String errorMsg    = nvl(timing.getErrorMessage(), null);
        String stackTrace  = nvl(timing.getStackTrace(), null);
        String status      = nvl(timing.getStatus(), "UNKNOWN");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        sb.append("<title>Trace: ").append(escHtml(testName)).append("</title>\n");
        sb.append(CSS);
        sb.append("</head>\n<body>\n");

        // ── Header ──
        sb.append("<div class=\"header\">\n");
        sb.append("  <div class=\"header-title\">\n");
        sb.append("    <span class=\"trace-icon\">&#x1F50D;</span> ");
        sb.append("    <span>Trace: <strong>").append(escHtml(testName)).append("</strong></span>\n");
        sb.append("    <span class=\"status-badge status-").append(status.toLowerCase()).append("\">")
          .append(escHtml(status)).append("</span>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"meta-grid\">\n");
        appendMeta(sb, "Class",    className);
        appendMeta(sb, "Browser",  browser);
        appendMeta(sb, "Duration", durationStr);
        appendMeta(sb, "Generated", timestamp);
        sb.append("  </div>\n</div>\n");

        // ── Step timeline ──
        sb.append("<div class=\"section\">\n");
        sb.append("  <div class=\"section-title\">Step Timeline");
        sb.append("  <span class=\"step-count\">(").append(steps.size()).append(" step").append(steps.size() != 1 ? "s" : "").append(")</span></div>\n");

        if (steps.isEmpty()) {
            sb.append("  <div class=\"no-steps\">No steps were logged. Use <code>StepLogger.step(\"name\", true)</code> to capture steps with screenshots.</div>\n");
        } else {
            // Compute the total timeline width for percentage layout
            long maxOffset = steps.stream().mapToLong(StepRecord::getOffsetMs).max().orElse(1L);
            if (maxOffset == 0) maxOffset = 1L;

            sb.append("  <div class=\"timeline\">\n");
            for (int i = 0; i < steps.size(); i++) {
                StepRecord s = steps.get(i);
                String stepStatus = nvl(s.getStatus(), "INFO").toLowerCase();
                boolean hasScreenshot = s.getScreenshotBase64() != null;
                String clickAttr = hasScreenshot
                        ? " onclick=\"showScreenshot(" + i + ")\" style=\"cursor:pointer\""
                        : "";
                sb.append("    <div class=\"step step-").append(stepStatus).append("\"")
                  .append(clickAttr)
                  .append(" title=\"").append(escHtml(s.getName())).append(" (+").append(s.getOffsetMs()).append("ms)\">\n");
                sb.append("      <div class=\"step-label\">").append(escHtml(s.getName())).append("</div>\n");
                sb.append("      <div class=\"step-meta\">+").append(s.getOffsetMs()).append("ms");
                if (hasScreenshot) sb.append(" &#x1F4F7;");
                sb.append("</div>\n");
                sb.append("    </div>\n");
            }
            sb.append("  </div>\n"); // timeline

            // ── Screenshot panel (hidden until a step is clicked) ──
            sb.append("  <div class=\"screenshot-panel\" id=\"screenshot-panel\">\n");
            sb.append("    <div class=\"screenshot-panel-header\">\n");
            sb.append("      <span id=\"screenshot-label\">Screenshot</span>\n");
            sb.append("      <button class=\"close-btn\" onclick=\"closeScreenshot()\">&#x2715; Close</button>\n");
            sb.append("    </div>\n");
            sb.append("    <img id=\"screenshot-img\" src=\"\" alt=\"Step screenshot\" />\n");
            sb.append("  </div>\n");

            // ── Embed screenshot data as JS array ──
            sb.append("  <script>\n  var stepScreenshots = [\n");
            for (int i = 0; i < steps.size(); i++) {
                StepRecord s = steps.get(i);
                if (s.getScreenshotBase64() != null) {
                    sb.append("    {idx:").append(i)
                      .append(", name:\"").append(escJs(s.getName())).append("\"")
                      .append(", src:\"data:image/png;base64,").append(s.getScreenshotBase64()).append("\"}");
                } else {
                    sb.append("    {idx:").append(i)
                      .append(", name:\"").append(escJs(s.getName())).append("\"")
                      .append(", src:null}");
                }
                if (i < steps.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ];\n  </script>\n");
        }
        sb.append("</div>\n"); // section

        // ── Final screenshot ──
        if (finalScreenshotBase64 != null) {
            sb.append("<div class=\"section\">\n");
            sb.append("  <div class=\"section-title\">Final State Screenshot</div>\n");
            sb.append("  <div class=\"final-screenshot-wrap\">\n");
            sb.append("    <img class=\"final-screenshot\" src=\"data:image/png;base64,")
              .append(finalScreenshotBase64).append("\" alt=\"Final state\" />\n");
            sb.append("  </div>\n</div>\n");
        }

        // ── Error section ──
        if (errorMsg != null || stackTrace != null) {
            sb.append("<div class=\"section error-section\">\n");
            sb.append("  <div class=\"section-title error-title\">&#x26A0; Failure Details</div>\n");
            if (errorMsg != null) {
                sb.append("  <div class=\"error-msg\">").append(escHtml(errorMsg)).append("</div>\n");
            }
            if (stackTrace != null) {
                sb.append("  <pre class=\"stack-trace\">").append(escHtml(stackTrace)).append("</pre>\n");
            }
            sb.append("</div>\n");
        }

        sb.append(JS);
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static File traceFile(String className, String testName) {
        return new File("target/traces/" + className + "/" + sanitize(testName) + "-trace.html");
    }

    static String captureFinalScreenshot(String testName) {
        try {
            WebDriver driver = DriverManager.getDriver();
            if (driver == null) return null;
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitize(String s) {
        return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static String nvl(String s, String def) {
        return (s != null && !s.isEmpty()) ? s : def;
    }

    private static void appendMeta(StringBuilder sb, String label, String value) {
        sb.append("    <div class=\"meta-item\"><span class=\"meta-label\">")
          .append(escHtml(label)).append("</span><span class=\"meta-value\">")
          .append(escHtml(value != null ? value : "—")).append("</span></div>\n");
    }

    static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // ------------------------------------------------------------------
    // Inlined CSS
    // ------------------------------------------------------------------

    private static final String CSS =
        "<style>\n" +
        "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
        "       background: #0d1117; color: #c9d1d9; font-size: 14px; line-height: 1.5; padding: 0 0 40px; }\n" +
        ".header { background: #161b22; border-bottom: 1px solid #30363d; padding: 20px 32px; }\n" +
        ".header-title { display: flex; align-items: center; gap: 10px; font-size: 18px;\n" +
        "                margin-bottom: 14px; flex-wrap: wrap; }\n" +
        ".trace-icon { font-size: 22px; }\n" +
        ".meta-grid { display: flex; flex-wrap: wrap; gap: 20px; }\n" +
        ".meta-item { display: flex; gap: 6px; }\n" +
        ".meta-label { color: #8b949e; font-size: 12px; }\n" +
        ".meta-value { color: #e6edf3; font-size: 12px; font-weight: 600; }\n" +
        ".section { margin: 24px 32px 0; background: #161b22; border: 1px solid #30363d;\n" +
        "           border-radius: 8px; overflow: hidden; }\n" +
        ".section-title { font-size: 15px; font-weight: 600; padding: 14px 20px;\n" +
        "                 border-bottom: 1px solid #30363d; color: #e6edf3;\n" +
        "                 background: #1c2128; display: flex; align-items: center; gap: 8px; }\n" +
        ".step-count { font-weight: 400; color: #8b949e; font-size: 13px; }\n" +
        ".timeline { display: flex; flex-wrap: wrap; gap: 8px; padding: 16px 20px; }\n" +
        ".step { border-radius: 6px; padding: 10px 14px; min-width: 140px; max-width: 280px;\n" +
        "        border-left: 3px solid #30363d; background: #0d1117;\n" +
        "        transition: transform .12s, box-shadow .12s; }\n" +
        ".step:hover { transform: translateY(-1px);\n" +
        "              box-shadow: 0 4px 12px rgba(0,0,0,.4); }\n" +
        ".step.active { outline: 2px solid #58a6ff; outline-offset: 2px; }\n" +
        ".step-info  { border-left-color: #58a6ff; }\n" +
        ".step-pass  { border-left-color: #3fb950; }\n" +
        ".step-fail  { border-left-color: #f85149; }\n" +
        ".step-warn  { border-left-color: #d29922; }\n" +
        ".step-label { font-size: 13px; color: #e6edf3; word-break: break-word; margin-bottom: 4px; }\n" +
        ".step-meta  { font-size: 11px; color: #8b949e; }\n" +
        ".no-steps { padding: 20px; color: #8b949e; font-style: italic; }\n" +
        ".screenshot-panel { display: none; margin: 0 20px 20px; background: #0d1117;\n" +
        "                    border: 1px solid #30363d; border-radius: 6px; overflow: hidden; }\n" +
        ".screenshot-panel-header { display: flex; justify-content: space-between;\n" +
        "                           align-items: center; padding: 10px 16px;\n" +
        "                           background: #1c2128; border-bottom: 1px solid #30363d;\n" +
        "                           font-size: 13px; color: #e6edf3; }\n" +
        ".close-btn { background: #21262d; border: 1px solid #30363d; color: #c9d1d9;\n" +
        "             border-radius: 4px; padding: 4px 10px; cursor: pointer; font-size: 12px; }\n" +
        ".close-btn:hover { background: #30363d; }\n" +
        "#screenshot-img { display: block; max-width: 100%; height: auto; padding: 12px; }\n" +
        ".final-screenshot-wrap { padding: 16px 20px; }\n" +
        ".final-screenshot { max-width: 100%; border-radius: 4px;\n" +
        "                    border: 1px solid #30363d; display: block; }\n" +
        ".error-section { }\n" +
        ".error-title { color: #f85149 !important; }\n" +
        ".error-msg { padding: 14px 20px; color: #f85149; font-weight: 600;\n" +
        "             font-size: 13px; border-bottom: 1px solid #30363d;\n" +
        "             background: rgba(248,81,73,.06); word-break: break-word; }\n" +
        ".stack-trace { padding: 16px 20px; font-size: 12px; color: #8b949e;\n" +
        "               font-family: 'SFMono-Regular', Consolas, monospace;\n" +
        "               white-space: pre-wrap; word-break: break-all;\n" +
        "               max-height: 400px; overflow-y: auto; line-height: 1.6; }\n" +
        ".status-badge { display: inline-block; padding: 2px 10px; border-radius: 12px;\n" +
        "                font-size: 12px; font-weight: 600; letter-spacing: .4px; }\n" +
        ".status-passed  { background: rgba(63,185,80,.15); color: #3fb950; }\n" +
        ".status-failed  { background: rgba(248,81,73,.15); color: #f85149; }\n" +
        ".status-skipped { background: rgba(210,153,34,.15); color: #d29922; }\n" +
        ".status-unknown { background: rgba(139,148,158,.15); color: #8b949e; }\n" +
        "</style>\n";

    // ------------------------------------------------------------------
    // Inlined JS
    // ------------------------------------------------------------------

    private static final String JS =
        "<script>\n" +
        "function showScreenshot(idx) {\n" +
        "  var entry = null;\n" +
        "  for (var i = 0; i < stepScreenshots.length; i++) {\n" +
        "    if (stepScreenshots[i].idx === idx) { entry = stepScreenshots[i]; break; }\n" +
        "  }\n" +
        "  if (!entry || !entry.src) return;\n" +
        "  document.getElementById('screenshot-img').src = entry.src;\n" +
        "  document.getElementById('screenshot-label').textContent = entry.name;\n" +
        "  var panel = document.getElementById('screenshot-panel');\n" +
        "  panel.style.display = 'block';\n" +
        "  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });\n" +
        "  document.querySelectorAll('.step').forEach(function(el){ el.classList.remove('active'); });\n" +
        "  document.querySelectorAll('.step')[idx].classList.add('active');\n" +
        "}\n" +
        "function closeScreenshot() {\n" +
        "  document.getElementById('screenshot-panel').style.display = 'none';\n" +
        "  document.querySelectorAll('.step').forEach(function(el){ el.classList.remove('active'); });\n" +
        "}\n" +
        "</script>\n";
}
