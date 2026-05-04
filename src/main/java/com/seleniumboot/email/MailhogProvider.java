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
 * Email provider backed by Mailhog's HTTP API (port 8025).
 * Mailhog is free, open-source, and Docker-friendly — ideal for local dev and CI.
 *
 * <pre>
 * email:
 *   provider: mailhog
 *   mailhog:
 *     host: localhost
 *     port: 8025
 * </pre>
 *
 * Run locally: {@code docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog}
 */
final class MailhogProvider implements EmailProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    MailhogProvider(SeleniumBootConfig.Email.Mailhog cfg) {
        this.baseUrl = "http://" + cfg.getHost() + ":" + cfg.getPort();
    }

    @Override
    public List<Email> fetchAll() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v2/messages?limit=200"))
                    .GET().build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            JsonNode root = JSON.readTree(body);
            JsonNode items = root.path("items");

            List<Email> emails = new ArrayList<>();
            for (JsonNode item : items) {
                emails.add(parseItem(item));
            }
            return emails;
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Mailhog] Failed to fetch messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/messages"))
                    .DELETE().build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new IllegalStateException("[Email/Mailhog] Failed to clear messages: " + e.getMessage(), e);
        }
    }

    private Email parseItem(JsonNode item) {
        JsonNode headers = item.path("Content").path("Headers");

        String subject  = firstHeader(headers, "Subject");
        String from     = firstHeader(headers, "From");
        String toHeader = firstHeader(headers, "To");

        // Fall back to raw To if header not present
        if (toHeader.isEmpty()) {
            JsonNode rawTo = item.path("Raw").path("To");
            if (rawTo.isArray() && rawTo.size() > 0) toHeader = rawTo.get(0).asText();
        }

        String contentType = firstHeader(headers, "Content-Type").toLowerCase();
        String plainBody   = "";
        String htmlBody    = "";

        if (contentType.startsWith("text/html")) {
            htmlBody  = item.path("Content").path("Body").asText();
        } else if (contentType.startsWith("text/plain")) {
            plainBody = item.path("Content").path("Body").asText();
        } else {
            // Multipart — traverse MIME parts
            plainBody = extractPart(item.path("MIME").path("Parts"), "text/plain");
            htmlBody  = extractPart(item.path("MIME").path("Parts"), "text/html");
            if (plainBody.isEmpty() && htmlBody.isEmpty()) {
                plainBody = item.path("Content").path("Body").asText();
            }
        }

        return new Email(subject, plainBody, htmlBody, from, toHeader);
    }

    private static String firstHeader(JsonNode headers, String name) {
        JsonNode node = headers.path(name);
        if (node.isArray() && node.size() > 0) return node.get(0).asText();
        return "";
    }

    private static String extractPart(JsonNode parts, String contentType) {
        if (!parts.isArray()) return "";
        for (JsonNode part : parts) {
            String ct = firstHeader(part.path("Headers"), "Content-Type").toLowerCase();
            if (ct.startsWith(contentType)) return part.path("Body").asText();
            // Recurse into nested parts
            String nested = extractPart(part.path("Parts"), contentType);
            if (!nested.isEmpty()) return nested;
        }
        return "";
    }
}
