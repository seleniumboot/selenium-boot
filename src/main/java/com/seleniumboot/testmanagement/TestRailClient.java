package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal TestRail REST v2 client.
 * Uses {@code java.net.http.HttpClient} — no extra dependencies required.
 *
 * <p>Authentication: HTTP Basic (username + API key).
 */
final class TestRailClient {

    private static final Pattern RUN_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private final String  baseUrl;
    private final String  authHeader;
    private final HttpClient http;

    TestRailClient(SeleniumBootConfig.TestManagement.TestRail cfg) {
        this.baseUrl = cfg.getUrl().replaceAll("/+$", "");
        String credentials = cfg.getUsername() + ":" + cfg.getApiKey();
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.http = HttpClient.newHttpClient();
    }

    /**
     * Creates a new test run and returns its ID.
     * Sends POST /index.php?/api/v2/add_run/{projectId}
     */
    int createRun(int projectId, int suiteId, String runName) {
        String body = buildRunJson(suiteId, runName);
        String url  = baseUrl + "/index.php?/api/v2/add_run/" + projectId;
        String response = post(url, body);
        return extractId(response);
    }

    /**
     * Posts a single test result.
     * Sends POST /index.php?/api/v2/add_result_for_case/{runId}/{caseId}
     *
     * @param status "PASSED", "FAILED", or "SKIPPED"
     * @param comment optional comment or error message (may be null)
     */
    void addResult(int runId, int caseId, String status, String comment) {
        int statusId = toStatusId(status);
        String body  = buildResultJson(statusId, comment);
        String url   = baseUrl + "/index.php?/api/v2/add_result_for_case/" + runId + "/" + caseId;
        post(url, body);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String post(String url, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "[TestRail] HTTP " + response.statusCode() + " for " + url + ": " + response.body());
            }
            return response.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[TestRail] Request failed: " + url, e);
        }
    }

    /** Extracts the first "id" value from the JSON response (naive but avoids a full JSON parser dep). */
    private int extractId(String json) {
        Matcher m = RUN_ID_PATTERN.matcher(json);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new IllegalStateException("[TestRail] Could not parse run id from response: " + json);
    }

    private String buildRunJson(int suiteId, String runName) {
        String escaped = runName.replace("\"", "\\\"");
        if (suiteId > 0) {
            return "{\"name\":\"" + escaped + "\",\"suite_id\":" + suiteId + ",\"include_all\":true}";
        }
        return "{\"name\":\"" + escaped + "\",\"include_all\":true}";
    }

    private String buildResultJson(int statusId, String comment) {
        if (comment == null || comment.isBlank()) {
            return "{\"status_id\":" + statusId + "}";
        }
        // Escape special chars so we don't break the JSON string
        String escaped = comment
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"status_id\":" + statusId + ",\"comment\":\"" + escaped + "\"}";
    }

    /**
     * Maps Selenium Boot status strings to TestRail status IDs.
     * Default TestRail statuses: 1=Passed, 2=Blocked, 3=Untested, 4=Retest, 5=Failed.
     */
    private int toStatusId(String status) {
        return switch (status.toUpperCase()) {
            case "PASSED"  -> 1;
            case "FAILED"  -> 5;
            default        -> 4; // SKIPPED → Retest
        };
    }
}
