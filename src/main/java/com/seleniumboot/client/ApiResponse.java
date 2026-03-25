package com.seleniumboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;

/**
 * Rich wrapper around an HTTP response.
 *
 * <pre>
 * ApiResponse res = apiClient().get("/api/users/1").send();
 * res.assertStatus(200);
 * String name = res.json("$.user.name");
 * User user   = res.asObject(User.class);
 * </pre>
 */
public class ApiResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpResponse<String> response;
    private final long durationMs;
    private JsonNode parsedBody;

    ApiResponse(HttpResponse<String> response, long durationMs) {
        this.response   = response;
        this.durationMs = durationMs;
    }

    /** HTTP status code. */
    public int status() {
        return response.statusCode();
    }

    /** Raw response body as String. */
    public String body() {
        return response.body();
    }

    /** Duration of the request in milliseconds. */
    public long durationMs() {
        return durationMs;
    }

    /** Response header value, or null if absent. */
    public String header(String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    /**
     * JSONPath-style extraction (supports {@code $.field}, {@code $.a.b}, {@code $.items[0].name}).
     * Returns null if the path is not found.
     */
    public String json(String path) {
        JsonNode node = jsonNode(path);
        return node == null || node.isMissingNode() ? null : node.asText();
    }

    /** JSONPath extraction with type conversion. */
    @SuppressWarnings("unchecked")
    public <T> T json(String path, Class<T> type) {
        JsonNode node = jsonNode(path);
        if (node == null || node.isMissingNode()) return null;
        try {
            return MAPPER.treeToValue(node, type);
        } catch (Exception e) {
            throw new RuntimeException("[ApiResponse] Cannot convert '" + path + "' to " + type.getSimpleName(), e);
        }
    }

    /** Deserialise entire response body to a POJO. */
    public <T> T asObject(Class<T> type) {
        try {
            return MAPPER.readValue(response.body(), type);
        } catch (Exception e) {
            throw new RuntimeException("[ApiResponse] Cannot deserialise body to " + type.getSimpleName(), e);
        }
    }

    // ── Fluent assertions ────────────────────────────────────────────────────

    /** Fails the test if status does not match. */
    public ApiResponse assertStatus(int expected) {
        if (response.statusCode() != expected) {
            throw new AssertionError(
                "[ApiResponse] Expected status " + expected +
                " but got " + response.statusCode() +
                ". Body: " + truncate(response.body(), 300));
        }
        return this;
    }

    /** Fails the test if the response body does not contain the given substring. */
    public ApiResponse assertBodyContains(String substring) {
        if (!response.body().contains(substring)) {
            throw new AssertionError(
                "[ApiResponse] Body does not contain: '" + substring + "'. " +
                "Body: " + truncate(response.body(), 300));
        }
        return this;
    }

    /** Fails the test if the JSONPath value does not equal expected. */
    public ApiResponse assertJson(String path, Object expected) {
        String actual = json(path);
        String expectedStr = String.valueOf(expected);
        if (!expectedStr.equals(actual)) {
            throw new AssertionError(
                "[ApiResponse] JSON path '" + path + "': expected '" + expectedStr +
                "' but got '" + actual + "'");
        }
        return this;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private JsonNode jsonNode(String path) {
        try {
            if (parsedBody == null) {
                parsedBody = MAPPER.readTree(response.body());
            }
            String pointer = toPointer(path);
            return parsedBody.at(pointer);
        } catch (Exception e) {
            throw new RuntimeException("[ApiResponse] Failed to parse JSON body. Body: " + truncate(response.body(), 200), e);
        }
    }

    /**
     * Converts JSONPath notation to Jackson JsonPointer.
     * {@code $.user.id}       → {@code /user/id}
     * {@code $.items[0].name} → {@code /items/0/name}
     */
    private String toPointer(String path) {
        return path.replaceFirst("^\\$", "")
                   .replace(".", "/")
                   .replaceAll("\\[(\\d+)]", "/$1")
                   .replaceFirst("^([^/])", "/$1");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    @Override
    public String toString() {
        return "ApiResponse{status=" + status() + ", durationMs=" + durationMs + "}";
    }
}
