package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xray REST client supporting both Cloud (Jira Cloud) and Server/Data-Center modes.
 *
 * <ul>
 *   <li><b>Cloud</b>  — authenticates via OAuth2 client credentials against
 *       {@code https://xray.cloud.getxpecto.com/api/v2/authenticate} and POSTs
 *       execution results to the same host.</li>
 *   <li><b>Server</b> — uses HTTP Basic (Jira username + password) and POSTs
 *       to {@code {jiraUrl}/rest/raven/1.0/import/execution}.</li>
 * </ul>
 */
final class XrayClient {

    private static final String CLOUD_AUTH_URL   = "https://xray.cloud.getxpecto.com/api/v2/authenticate";
    private static final String CLOUD_IMPORT_URL = "https://xray.cloud.getxpecto.com/api/v2/import/execution";
    private static final Pattern TOKEN_PATTERN   = Pattern.compile("\"(.*?)\"");

    private final SeleniumBootConfig.TestManagement.Xray cfg;
    private final HttpClient http;
    private final boolean isCloud;

    XrayClient(SeleniumBootConfig.TestManagement.Xray cfg) {
        this.cfg     = cfg;
        this.http    = HttpClient.newHttpClient();
        this.isCloud = "cloud".equalsIgnoreCase(cfg.getMode());
    }

    /**
     * Imports a batch of test results into Xray.
     *
     * @param results list of completed test results for this suite
     */
    void importExecution(List<XrayTestResult> results) {
        if (results.isEmpty()) return;

        String importUrl;
        String authHeader;

        if (isCloud) {
            String token = authenticateCloud();
            authHeader   = "Bearer " + token;
            importUrl    = CLOUD_IMPORT_URL;
        } else {
            String credentials = cfg.getUsername() + ":" + cfg.getPassword();
            authHeader  = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            importUrl   = cfg.getJiraUrl().replaceAll("/+$", "") + "/rest/raven/1.0/import/execution";
        }

        String body = buildExecutionJson(results);
        post(importUrl, authHeader, body);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String authenticateCloud() {
        String body = "{\"client_id\":\"" + esc(cfg.getClientId())
                    + "\",\"client_secret\":\"" + esc(cfg.getClientSecret()) + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_AUTH_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "[Xray] Cloud authentication failed (HTTP " + response.statusCode() + "): " + response.body());
            }
            // Response is a JSON string: "\"<token>\""
            Matcher m = TOKEN_PATTERN.matcher(response.body().trim());
            if (m.find()) return m.group(1);
            throw new IllegalStateException("[Xray] Could not parse auth token from response: " + response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[Xray] Authentication request failed", e);
        }
    }

    private void post(String url, String authHeader, String body) {
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
                        "[Xray] HTTP " + response.statusCode() + " for " + url + ": " + response.body());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[Xray] Request failed: " + url, e);
        }
    }

    private String buildExecutionJson(List<XrayTestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (cfg.getProjectKey() != null && !cfg.getProjectKey().isBlank()) {
            sb.append("\"info\":{\"project\":\"").append(esc(cfg.getProjectKey())).append("\"");
            sb.append(",\"summary\":\"Selenium Boot automated run\"");
            if (cfg.getTestPlanKey() != null && !cfg.getTestPlanKey().isBlank()) {
                sb.append(",\"testPlanKey\":\"").append(esc(cfg.getTestPlanKey())).append("\"");
            }
            sb.append("},");
        }

        sb.append("\"tests\":[");
        for (int i = 0; i < results.size(); i++) {
            XrayTestResult r = results.get(i);
            sb.append("{\"testKey\":\"").append(esc(r.testKey()))
              .append("\",\"status\":\"").append(toXrayStatus(r.status())).append("\"");
            if (r.comment() != null && !r.comment().isBlank()) {
                sb.append(",\"comment\":\"").append(esc(r.comment())).append("\"");
            }
            sb.append("}");
            if (i < results.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String toXrayStatus(String status) {
        return switch (status.toUpperCase()) {
            case "PASSED"  -> "PASS";
            case "FAILED"  -> "FAIL";
            default        -> "TODO";
        };
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
