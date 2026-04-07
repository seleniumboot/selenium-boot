package com.seleniumboot.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seleniumboot.config.SeleniumBootConfig.Notifications;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ReportAdapter} that posts a suite summary to Slack and/or Microsoft Teams
 * after each suite run via incoming webhooks.
 *
 * <p>Enable via {@code selenium-boot.yml}:
 * <pre>
 * notifications:
 *   slack:
 *     webhookUrl: https://hooks.slack.com/services/...
 *     notifyOnFailureOnly: false   # default
 *   teams:
 *     webhookUrl: https://xxx.webhook.office.com/webhookb2/...
 *     notifyOnFailureOnly: false   # default
 * </pre>
 *
 * <p>The message includes: overall status, pass rate, passed/failed/skipped counts,
 * total duration, and the names of up to 5 failed tests.
 */
public class NotificationAdapter implements ReportAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_FAILED_LISTED = 5;

    private final Notifications config;

    /** Used by {@link com.seleniumboot.lifecycle.FrameworkBootstrap} at init time. */
    public NotificationAdapter(Notifications config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "notification";
    }

    @Override
    public void generate(File metricsJson) {
        if (!metricsJson.exists()) return;

        try {
            JsonNode metrics = MAPPER.readTree(metricsJson);
            int failed = metrics.path("failedTests").asInt(0);

            Notifications.Slack slack = config.getSlack();
            if (slack != null && hasUrl(slack.getWebhookUrl())) {
                if (!slack.isNotifyOnFailureOnly() || failed > 0) {
                    String payload = buildSlackPayload(metrics);
                    int status = sendWebhook(slack.getWebhookUrl(), payload);
                    System.out.println("[Selenium Boot] Slack notification sent (HTTP " + status + ")");
                }
            }

            Notifications.Teams teams = config.getTeams();
            if (teams != null && hasUrl(teams.getWebhookUrl())) {
                if (!teams.isNotifyOnFailureOnly() || failed > 0) {
                    String payload = buildTeamsPayload(metrics);
                    int status = sendWebhook(teams.getWebhookUrl(), payload);
                    System.out.println("[Selenium Boot] Teams notification sent (HTTP " + status + ")");
                }
            }

        } catch (Exception e) {
            System.err.println("[Selenium Boot] Notification adapter failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // Payload builders
    // ----------------------------------------------------------

    String buildSlackPayload(JsonNode metrics) {
        int passed   = metrics.path("passedTests").asInt(0);
        int failed   = metrics.path("failedTests").asInt(0);
        int skipped  = metrics.path("skippedTests").asInt(0);
        double rate  = metrics.path("passRate").asDouble(0);
        long totalMs = metrics.path("totalTimeMs").asLong(0);

        boolean ok  = failed == 0;
        String icon = ok ? "✅" : "❌";

        ObjectNode payload = MAPPER.createObjectNode();
        ArrayNode  blocks  = payload.putArray("blocks");

        // Header
        ObjectNode header = blocks.addObject();
        header.put("type", "header");
        header.putObject("text")
              .put("type", "plain_text")
              .put("text", icon + " Selenium Boot — Suite Results");

        // Stats fields
        ObjectNode section = blocks.addObject();
        section.put("type", "section");
        ArrayNode fields = section.putArray("fields");
        addSlackField(fields, "*Status*",    ok ? "✅ PASSED" : "❌ FAILED");
        addSlackField(fields, "*Pass Rate*", String.format("%.1f%%", rate));
        addSlackField(fields, "*Tests*",
                passed + " passed  " + failed + " failed  " + skipped + " skipped");
        addSlackField(fields, "*Duration*",  formatDuration(totalMs));

        // Failed test list (up to MAX_FAILED_LISTED)
        List<String> failedNames = failedTestNames(metrics);
        if (!failedNames.isEmpty()) {
            StringBuilder sb = new StringBuilder("*Failed Tests:*\n");
            for (String name : failedNames) sb.append("• ").append(name).append("\n");
            if (failed > MAX_FAILED_LISTED) {
                sb.append("• _...and ").append(failed - MAX_FAILED_LISTED).append(" more_");
            }
            blocks.addObject()
                  .put("type", "section")
                  .putObject("text")
                  .put("type", "mrkdwn")
                  .put("text", sb.toString().trim());
        }

        return payload.toString();
    }

    String buildTeamsPayload(JsonNode metrics) {
        int passed   = metrics.path("passedTests").asInt(0);
        int failed   = metrics.path("failedTests").asInt(0);
        int skipped  = metrics.path("skippedTests").asInt(0);
        double rate  = metrics.path("passRate").asDouble(0);
        long totalMs = metrics.path("totalTimeMs").asLong(0);

        boolean ok     = failed == 0;
        String  color  = ok ? "36a64f" : "e01e5a";
        String  status = ok ? "✅ PASSED" : "❌ FAILED";

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("@type",      "MessageCard");
        payload.put("@context",   "http://schema.org/extensions");
        payload.put("themeColor", color);
        payload.put("summary",    "Selenium Boot Suite: " + status);

        ArrayNode sections = payload.putArray("sections");
        ObjectNode section = sections.addObject();
        section.put("activityTitle",    "**Selenium Boot — Suite Results**");
        section.put("activitySubtitle", status);

        ArrayNode facts = section.putArray("facts");
        addFact(facts, "Pass Rate", String.format("%.1f%%", rate));
        addFact(facts, "Tests",
                passed + " passed, " + failed + " failed, " + skipped + " skipped");
        addFact(facts, "Duration", formatDuration(totalMs));

        List<String> failedNames = failedTestNames(metrics);
        if (!failedNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String name : failedNames) sb.append("• ").append(name).append("<br>");
            if (failed > MAX_FAILED_LISTED) {
                sb.append("• ...and ").append(failed - MAX_FAILED_LISTED).append(" more");
            }
            addFact(facts, "Failed Tests", sb.toString());
        }

        return payload.toString();
    }

    // ----------------------------------------------------------
    // HTTP
    // ----------------------------------------------------------

    /**
     * POSTs {@code payload} as JSON to {@code webhookUrl}.
     * Override in tests to capture calls without making real HTTP requests.
     *
     * @return HTTP response status code
     */
    protected int sendWebhook(String webhookUrl, String payload)
            throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private List<String> failedTestNames(JsonNode metrics) {
        List<String> names = new ArrayList<>();
        for (JsonNode test : metrics.path("tests")) {
            if ("FAILED".equals(test.path("status").asText()) && names.size() < MAX_FAILED_LISTED) {
                names.add(test.path("testId").asText());
            }
        }
        return names;
    }

    private void addSlackField(ArrayNode fields, String title, String value) {
        fields.addObject()
              .put("type", "mrkdwn")
              .put("text", title + "\n" + value);
    }

    private void addFact(ArrayNode facts, String name, String value) {
        ObjectNode fact = facts.addObject();
        fact.put("name",  name);
        fact.put("value", value);
    }

    private String formatDuration(long totalMs) {
        if (totalMs < 1_000) return totalMs + "ms";
        if (totalMs < 60_000) return String.format("%.1fs", totalMs / 1000.0);
        long mins = totalMs / 60_000;
        long secs = (totalMs % 60_000) / 1000;
        return mins + "m " + secs + "s";
    }

    private boolean hasUrl(String url) {
        return url != null && !url.isBlank();
    }
}
