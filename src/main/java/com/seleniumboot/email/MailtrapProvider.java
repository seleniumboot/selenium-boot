package com.seleniumboot.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Email provider backed by the Mailtrap v1 API.
 * Mailtrap is a hosted email sandbox — useful for shared CI environments.
 *
 * <pre>
 * email:
 *   provider: mailtrap
 *   mailtrap:
 *     apiToken: ${MAILTRAP_TOKEN}
 *     accountId: ${MAILTRAP_ACCOUNT_ID}   # optional for v1
 *     inboxId:   ${MAILTRAP_INBOX_ID}
 * </pre>
 */
final class MailtrapProvider implements EmailProvider {

    private static final ObjectMapper JSON       = new ObjectMapper();
    private static final String       BASE_URL   = "https://mailtrap.io/api/v1";

    private final String apiToken;
    private final String inboxId;
    private final HttpClient http = HttpClient.newHttpClient();

    MailtrapProvider(SeleniumBootConfig.Email.Mailtrap cfg) {
        this.apiToken = resolveEnv(cfg.getApiToken());
        this.inboxId  = resolveEnv(cfg.getInboxId());
        if (apiToken == null || apiToken.isEmpty())
            throw new IllegalStateException("[Email/Mailtrap] mailtrap.apiToken is required");
        if (inboxId  == null || inboxId.isEmpty())
            throw new IllegalStateException("[Email/Mailtrap] mailtrap.inboxId is required");
    }

    @Override
    public List<Email> fetchAll() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/inboxes/" + inboxId + "/messages"))
                    .header("Api-Token", apiToken)
                    .GET().build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = JSON.readTree(body);

            List<Email> emails = new ArrayList<>();
            for (JsonNode item : root) {
                emails.add(new Email(
                    item.path("subject").asText(),
                    item.path("text_body").asText(),
                    item.path("html_body").asText(),
                    item.path("from_email").asText(),
                    item.path("to_email").asText()
                ));
            }
            return emails;
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Mailtrap] Failed to fetch messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/inboxes/" + inboxId + "/clean"))
                    .header("Api-Token", apiToken)
                    .method("PATCH", HttpRequest.BodyPublishers.noBody()).build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Mailtrap] Failed to clear inbox: " + e.getMessage(), e);
        }
    }

    private static String resolveEnv(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String var = value.substring(2, value.length() - 1);
            String resolved = System.getenv(var);
            return resolved != null ? resolved : System.getProperty(var, value);
        }
        return value;
    }
}
