package com.seleniumboot.precondition;

import org.testng.SkipException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performs HTTP health checks for {@link DependsOnApi}.
 *
 * <p>Results are cached per URL for the entire suite run — the same endpoint
 * is probed at most once. The cache is cleared at suite start by
 * {@link com.seleniumboot.listeners.SuiteExecutionListener}.
 */
public final class ApiHealthChecker {

    // URL → isUp; shared across all threads within a suite run
    private static final ConcurrentHashMap<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private ApiHealthChecker() {}

    /**
     * Checks whether {@code url} is reachable. Throws {@link SkipException} if it is not,
     * causing TestNG to skip the test rather than fail it.
     *
     * <p>The result is cached: subsequent calls for the same URL within the same suite
     * return the cached result without making another HTTP request.
     */
    public static void checkOrSkip(String url, int timeoutSeconds) {
        boolean up = CACHE.computeIfAbsent(url, u -> probe(u, timeoutSeconds));
        if (!up) {
            throw new SkipException(
                    "@DependsOnApi: [" + url + "] is unreachable — test skipped");
        }
    }

    /** Clears the health-check cache. Called at suite start. */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Probes {@code url} with an HTTP GET request.
     * Returns {@code true} if the response is 2xx, {@code false} for any other
     * status code or connection error.
     *
     * <p>Package-private to allow override in unit tests.
     */
    static boolean probe(String url, int timeoutSeconds) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            boolean up = status >= 200 && status < 300;
            if (!up) {
                System.out.println("[Selenium Boot] @DependsOnApi: " + url
                        + " responded HTTP " + status + " — dependent tests will be skipped");
            }
            return up;
        } catch (Exception e) {
            System.out.println("[Selenium Boot] @DependsOnApi: " + url
                    + " is unreachable (" + e.getMessage() + ") — dependent tests will be skipped");
            return false;
        }
    }
}
