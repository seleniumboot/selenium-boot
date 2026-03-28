package com.seleniumboot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent HTTP client for API testing — zero boilerplate, same philosophy as BasePage.
 *
 * <pre>
 * // Pure API call
 * ApiResponse res = apiClient().post("/api/login")
 *         .body(Map.of("username", "admin", "password", "pass"))
 *         .send();
 * res.assertStatus(200);
 * String token = res.json("$.token");
 *
 * // Different base URL
 * ApiResponse health = ApiClient.to("https://other-service.com")
 *         .get("/health")
 *         .send();
 * </pre>
 */
public class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Thread-local global auth — applied to every request on this thread unless overridden. */
    private static final ThreadLocal<ApiAuth> GLOBAL_AUTH = new ThreadLocal<>();

    /** Set once (e.g. in {@code @BeforeSuite}) — all requests on this thread use it automatically. */
    public static void setGlobalAuth(ApiAuth auth)  { GLOBAL_AUTH.set(auth); }

    /** Remove global auth for this thread. Called automatically by the framework after each test. */
    public static void clearGlobalAuth()            { GLOBAL_AUTH.remove(); }

    private String              baseUrl;
    private String              method;
    private String              path;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private Object              body;
    private ApiAuth             auth;

    private ApiClient() {}

    // ── Factory methods ───────────────────────────────────────────────────────

    /** Returns a blank ApiClient — use fluent methods to set method and path. */
    public static ApiClient create() { return new ApiClient(); }

    public static ApiClient get(String path)    { return method("GET",    path); }
    public static ApiClient post(String path)   { return method("POST",   path); }
    public static ApiClient put(String path)    { return method("PUT",    path); }
    public static ApiClient patch(String path)  { return method("PATCH",  path); }
    public static ApiClient delete(String path) { return method("DELETE", path); }

    /** Override base URL for this request only. */
    public static ApiClient to(String baseUrl) {
        ApiClient c = new ApiClient();
        c.baseUrl = baseUrl;
        return c;
    }

    public ApiClient get()    { this.method = "GET";    return this; }
    public ApiClient post()   { this.method = "POST";   return this; }
    public ApiClient put()    { this.method = "PUT";    return this; }
    public ApiClient patch()  { this.method = "PATCH";  return this; }
    public ApiClient delete() { this.method = "DELETE"; return this; }
    public ApiClient path(String path) { this.path = path; return this; }

    // ── Builder methods ───────────────────────────────────────────────────────

    public ApiClient header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ApiClient contentType(String contentType) {
        return header("Content-Type", contentType);
    }

    public ApiClient body(Object payload) {
        this.body = payload;
        return this;
    }

    public ApiClient auth(ApiAuth auth) {
        this.auth = auth;
        return this;
    }

    // ── Execute ───────────────────────────────────────────────────────────────

    public ApiResponse send() {
        String url     = isAbsolute(path) ? path
                       : resolveBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        int    timeout = resolveTimeout();

        try {
            String bodyStr = serializeBody();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            // Default content type for body requests
            if (bodyStr != null && !headers.containsKey("Content-Type")) {
                builder.header("Content-Type", "application/json");
            }
            headers.forEach(builder::header);
            ApiAuth effectiveAuth = this.auth != null ? this.auth : GLOBAL_AUTH.get();
            if (effectiveAuth != null) effectiveAuth.apply(builder);

            HttpRequest.BodyPublisher publisher = bodyStr != null
                    ? HttpRequest.BodyPublishers.ofString(bodyStr)
                    : HttpRequest.BodyPublishers.noBody();

            builder.method(method, publisher);

            long start    = System.currentTimeMillis();
            HttpResponse<String> raw = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            ApiResponse response = new ApiResponse(raw, duration);
            logStep(response);
            return response;

        } catch (Exception e) {
            StepLogger.step("[API] " + method + " " + url + " → ERROR: " + e.getMessage(), StepStatus.FAIL);
            throw new RuntimeException("[ApiClient] Request failed: " + method + " " + url, e);
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static ApiClient method(String method, String path) {
        ApiClient c = new ApiClient();
        c.method = method;
        c.path   = path;
        return c;
    }

    private String resolveBaseUrl() {
        if (baseUrl != null) return baseUrl;
        try {
            SeleniumBootConfig config = SeleniumBootContext.getConfig();
            SeleniumBootConfig.Api api = config.getApi();
            if (api != null && api.getBaseUrl() != null) return api.getBaseUrl();
            return config.getExecution().getBaseUrl();
        } catch (Exception e) {
            throw new IllegalStateException("[ApiClient] No baseUrl configured. Set execution.baseUrl or api.baseUrl in selenium-boot.yml");
        }
    }

    private int resolveTimeout() {
        try {
            SeleniumBootConfig.Api api = SeleniumBootContext.getConfig().getApi();
            return api != null ? api.getTimeoutSeconds() : 30;
        } catch (Exception e) {
            return 30;
        }
    }

    private String serializeBody() throws Exception {
        if (body == null) return null;
        if (body instanceof String) return (String) body;
        return MAPPER.writeValueAsString(body);
    }

    private void logStep(ApiResponse response) {
        boolean logBody = false;
        try {
            SeleniumBootConfig.Api api = SeleniumBootContext.getConfig().getApi();
            logBody = api != null && api.isLogBody();
        } catch (Exception ignored) {}

        StepStatus status = response.status() >= 400 ? StepStatus.FAIL : StepStatus.PASS;
        String log = "[API] " + method + " " + path + " → " + response.status() + " (" + response.durationMs() + "ms)";
        if (logBody && response.body() != null && !response.body().isBlank()) {
            log += "\n  Body: " + truncate(response.body(), 300);
        }
        StepLogger.step(log, status);
    }

    private boolean isAbsolute(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
