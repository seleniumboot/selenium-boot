package com.seleniumboot.ai;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.steps.StepRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Calls the Claude API to generate a plain-English failure analysis for a failed test.
 *
 * <p>Activated when {@code ai.failureAnalysis: true} and {@code ai.apiKey} are set in
 * {@code selenium-boot.yml}. The analysis is stored in the test metrics and surfaced in the
 * HTML report below the stack trace.
 *
 * <p>Uses {@code claude-haiku-4-5-20251001} by default (fast + low cost). Override via
 * {@code ai.model}.
 *
 * <p>The API call is synchronous but bounded by {@code ai.timeoutSeconds} (default 20s).
 * Any failure (network error, API error, timeout) is silently suppressed — the test suite
 * result is never affected by the AI analysis step.
 *
 * <pre>
 * # selenium-boot.yml
 * ai:
 *   failureAnalysis: true
 *   apiKey: ${CLAUDE_API_KEY}
 *   model: claude-haiku-4-5-20251001
 *   timeoutSeconds: 20
 * </pre>
 */
@SeleniumBootApi(since = "1.8.0")
public final class AiFailureAnalyzer {

    private static final Logger LOG = Logger.getLogger(AiFailureAnalyzer.class.getName());
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private AiFailureAnalyzer() {}

    // ------------------------------------------------------------------
    // Public API — called by TestExecutionListener
    // ------------------------------------------------------------------

    /**
     * Analyses the failure of {@code testId} and records the result via
     * {@link ExecutionMetrics#recordAiAnalysis}.
     *
     * @param testId   fully-qualified test method name
     * @param pageUrl  current page URL at the time of failure (may be null)
     * @param pageTitle current page title (may be null)
     */
    public static void analyze(String testId, String pageUrl, String pageTitle) {
        try {
            SeleniumBootConfig.Ai aiCfg = config();
            if (aiCfg == null || !aiCfg.isFailureAnalysis()) return;

            String apiKey = resolveApiKey(aiCfg.getApiKey());
            if (apiKey == null || apiKey.isEmpty()) {
                LOG.warning("[AiFailureAnalyzer] ai.apiKey is not configured. Skipping analysis.");
                return;
            }

            TestTiming timing = ExecutionMetrics.getTiming(testId);
            if (timing == null) return;

            String prompt = buildPrompt(timing, pageUrl, pageTitle);
            String analysis = callApi(apiKey, aiCfg.getModel(), prompt, aiCfg.getTimeoutSeconds());
            if (analysis != null && !analysis.isBlank()) {
                ExecutionMetrics.recordAiAnalysis(testId, analysis.strip());
                LOG.info("[AiFailureAnalyzer] Analysis recorded for: " + testId);
            }
        } catch (Exception e) {
            LOG.warning("[AiFailureAnalyzer] Analysis failed (non-critical): " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Prompt construction
    // ------------------------------------------------------------------

    static String buildPrompt(TestTiming timing, String pageUrl, String pageTitle) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a QA automation expert. A Selenium WebDriver test just failed.\n\n");

        sb.append("## Test Context\n");
        sb.append("- Test:     ").append(nvl(timing.getTestId(), "unknown")).append("\n");
        sb.append("- Class:    ").append(nvl(timing.getTestClassName(), "unknown")).append("\n");
        sb.append("- Browser:  ").append(nvl(timing.getBrowser(), "unknown")).append("\n");
        sb.append("- Duration: ").append(timing.getTotalTime()).append("ms\n");
        if (pageUrl   != null) sb.append("- URL:      ").append(pageUrl).append("\n");
        if (pageTitle != null) sb.append("- Title:    ").append(pageTitle).append("\n");

        if (timing.getErrorMessage() != null) {
            sb.append("\n## Error Message\n```\n")
              .append(timing.getErrorMessage()).append("\n```\n");
        }

        if (timing.getStackTrace() != null) {
            // Truncate to first 30 lines to stay within token budget
            String[] lines = timing.getStackTrace().split("\n");
            int limit = Math.min(lines.length, 30);
            sb.append("\n## Stack Trace (first ").append(limit).append(" lines)\n```\n");
            for (int i = 0; i < limit; i++) sb.append(lines[i]).append("\n");
            sb.append("```\n");
        }

        List<StepRecord> steps = timing.getSteps();
        if (!steps.isEmpty()) {
            sb.append("\n## Steps Executed\n");
            for (StepRecord s : steps) {
                sb.append("- [+").append(s.getOffsetMs()).append("ms] ")
                  .append(s.getStatus()).append(": ").append(s.getName()).append("\n");
            }
        }

        sb.append("\n## Your Task\n");
        sb.append("Provide a concise failure analysis in this exact format:\n\n");
        sb.append("**Root Cause:** (1-2 sentences explaining what likely went wrong)\n\n");
        sb.append("**Suggested Fix:**\n- (bullet 1)\n- (bullet 2, if needed)\n\n");
        sb.append("Be specific and actionable. Do not repeat the error message verbatim.");

        return sb.toString();
    }

    // ------------------------------------------------------------------
    // HTTP call to Claude API
    // ------------------------------------------------------------------

    static String callApi(String apiKey, String model, String prompt, int timeoutSeconds) {
        try {
            String body = buildRequestBody(model, prompt);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key",          apiKey)
                    .header("anthropic-version",  API_VERSION)
                    .header("content-type",       "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                LOG.warning("[AiFailureAnalyzer] API returned HTTP " + response.statusCode()
                        + ": " + response.body().substring(0, Math.min(200, response.body().length())));
                return null;
            }
        } catch (Exception e) {
            LOG.warning("[AiFailureAnalyzer] HTTP call failed: " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // JSON helpers (no extra dependency — hand-rolled)
    // ------------------------------------------------------------------

    private static String buildRequestBody(String model, String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
        return "{"
             + "\"model\":\"" + model + "\","
             + "\"max_tokens\":512,"
             + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]"
             + "}";
    }

    /** Extracts {@code content[0].text} from a Claude API response JSON string. */
    public static String extractContent(String json) {
        // Simple extraction — avoids adding Jackson dependency to the hot path
        // Pattern: "text": "..." (first occurrence in content array)
        int textIdx = json.indexOf("\"text\"");
        if (textIdx < 0) return null;
        int colon = json.indexOf(':', textIdx);
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        // Walk forward, handling \" escapes
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    default:   sb.append(next); i += 2; continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static SeleniumBootConfig.Ai config() {
        try { return SeleniumBootContext.getConfig().getAi(); } catch (Exception e) { return null; }
    }

    private static String resolveApiKey(String raw) {
        if (raw == null) return null;
        if (raw.startsWith("${") && raw.endsWith("}")) {
            String var = raw.substring(2, raw.length() - 1);
            String val = System.getenv(var);
            return val != null ? val : System.getProperty(var);
        }
        return raw;
    }

    private static String nvl(String s, String def) {
        return s != null && !s.isEmpty() ? s : def;
    }
}
