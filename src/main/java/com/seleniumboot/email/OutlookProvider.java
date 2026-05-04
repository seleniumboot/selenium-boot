package com.seleniumboot.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Email provider backed by Microsoft Graph API (app-only OAuth2 client credentials).
 * Works with Office 365 and personal Outlook accounts.
 * Does NOT require user sign-in — uses service account credentials.
 *
 * <p>Setup in Azure AD:
 * <ol>
 *   <li>Register an app (App registrations → New registration)</li>
 *   <li>Add API permission: Microsoft Graph → Application → {@code Mail.Read} + {@code Mail.ReadWrite}</li>
 *   <li>Grant admin consent</li>
 *   <li>Create a client secret</li>
 * </ol>
 *
 * <pre>
 * email:
 *   provider: outlook
 *   outlook:
 *     tenantId:     ${AZURE_TENANT_ID}
 *     clientId:     ${AZURE_CLIENT_ID}
 *     clientSecret: ${AZURE_CLIENT_SECRET}
 *     mailbox:      test-inbox@yourcompany.com
 * </pre>
 */
final class OutlookProvider implements EmailProvider {

    private static final ObjectMapper JSON         = new ObjectMapper();
    private static final String       GRAPH_BASE   = "https://graph.microsoft.com/v1.0";
    private static final String       TOKEN_URL    = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final long         TOKEN_BUFFER = 60_000L; // refresh 60s before expiry

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String mailbox;
    private final HttpClient http = HttpClient.newHttpClient();

    private volatile String accessToken;
    private volatile long   tokenExpiresAt = 0;

    OutlookProvider(SeleniumBootConfig.Email.Outlook cfg) {
        this.tenantId     = resolveEnv(cfg.getTenantId());
        this.clientId     = resolveEnv(cfg.getClientId());
        this.clientSecret = resolveEnv(cfg.getClientSecret());
        this.mailbox      = resolveEnv(cfg.getMailbox());
        validateConfig();
    }

    @Override
    public List<Email> fetchAll() {
        String token = getToken();
        String url   = GRAPH_BASE + "/users/" + encode(mailbox)
                + "/messages?$top=100&$orderby=receivedDateTime+desc"
                + "&$select=id,subject,from,toRecipients,body,receivedDateTime";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET().build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = JSON.readTree(body);

            List<Email> emails = new ArrayList<>();
            for (JsonNode msg : root.path("value")) {
                emails.add(parseMessage(msg));
            }
            return emails;
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Outlook] Failed to fetch messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        List<Email> all = fetchAll();
        if (all.isEmpty()) return;
        // Fetch IDs to delete (we need the raw ids, refetch with id only)
        String token = getToken();
        try {
            String listUrl = GRAPH_BASE + "/users/" + encode(mailbox)
                    + "/messages?$top=500&$select=id";
            HttpRequest listReq = HttpRequest.newBuilder()
                    .uri(URI.create(listUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET().build();
            String body = http.send(listReq, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = JSON.readTree(body);
            for (JsonNode msg : root.path("value")) {
                String id = msg.path("id").asText();
                if (!id.isEmpty()) deleteMessage(id, token);
            }
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Outlook] Failed to clear messages: " + e.getMessage(), e);
        }
    }

    // ── Token management ──────────────────────────────────────────────────

    private synchronized String getToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - TOKEN_BUFFER) {
            return accessToken;
        }
        try {
            String form = "grant_type=client_credentials"
                    + "&client_id="     + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(TOKEN_URL, tenantId)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();

            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = JSON.readTree(body);
            if (root.has("error")) {
                throw new IllegalStateException(root.path("error_description").asText(root.path("error").asText()));
            }
            accessToken    = root.path("access_token").asText();
            long expiresIn = root.path("expires_in").asLong(3600);
            tokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
            return accessToken;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Outlook] OAuth2 token request failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Email parseMessage(JsonNode msg) {
        String subject = msg.path("subject").asText();
        String from    = msg.path("from").path("emailAddress").path("address").asText();
        String to      = "";
        JsonNode toList = msg.path("toRecipients");
        if (toList.isArray() && toList.size() > 0) {
            to = toList.get(0).path("emailAddress").path("address").asText();
        }
        String contentType = msg.path("body").path("contentType").asText("text");
        String bodyContent = msg.path("body").path("content").asText();
        String plain = "html".equalsIgnoreCase(contentType) ? "" : bodyContent;
        String html  = "html".equalsIgnoreCase(contentType) ? bodyContent : "";
        return new Email(subject, plain, html, from, to);
    }

    private void deleteMessage(String id, String token) {
        try {
            String url = GRAPH_BASE + "/users/" + encode(mailbox) + "/messages/" + id;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .DELETE().build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private void validateConfig() {
        if (isBlank(tenantId))     throw new IllegalStateException("[Email/Outlook] outlook.tenantId is required");
        if (isBlank(clientId))     throw new IllegalStateException("[Email/Outlook] outlook.clientId is required");
        if (isBlank(clientSecret)) throw new IllegalStateException("[Email/Outlook] outlook.clientSecret is required");
        if (isBlank(mailbox))      throw new IllegalStateException("[Email/Outlook] outlook.mailbox is required");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
