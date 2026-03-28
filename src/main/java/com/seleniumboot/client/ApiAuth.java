package com.seleniumboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auth strategies for {@link ApiClient}.
 *
 * <pre>
 * // Per-request
 * ApiClient.get("/api/me").auth(ApiAuth.bearerToken("token")).send();
 *
 * // Suite-wide (apply once in @BeforeSuite, all requests use it automatically)
 * ApiClient.setGlobalAuth(ApiAuth.bearerToken(loginResponse.json("$.token")));
 *
 * // OAuth2 client credentials (token fetched + cached automatically)
 * ApiClient.setGlobalAuth(ApiAuth.oauth2(tokenUrl, clientId, clientSecret));
 * </pre>
 */
@FunctionalInterface
public interface ApiAuth {

    void apply(HttpRequest.Builder builder);

    // ── Static factories ──────────────────────────────────────────────────────

    /** Sets {@code Authorization: Bearer <token>}. */
    public static ApiAuth bearerToken(String token) {
        return builder -> builder.header("Authorization", "Bearer " + token);
    }

    /** Sets {@code Authorization: Basic <base64(user:pass)>}. */
    public static ApiAuth basicAuth(String username, String password) {
        return builder -> {
            String encoded = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
            builder.header("Authorization", "Basic " + encoded);
        };
    }

    /**
     * OAuth2 client credentials flow.
     * Token is fetched on first use and cached per {@code tokenUrl + clientId} until expiry.
     *
     * <pre>
     * ApiClient.setGlobalAuth(ApiAuth.oauth2(
     *     "https://auth.example.com/token",
     *     System.getenv("CLIENT_ID"),
     *     System.getenv("CLIENT_SECRET")
     * ));
     * </pre>
     */
    public static ApiAuth oauth2(String tokenUrl, String clientId, String clientSecret) {
        return builder -> {
            String token = OAuth2TokenCache.getToken(tokenUrl, clientId, clientSecret);
            builder.header("Authorization", "Bearer " + token);
        };
    }

    // ── OAuth2 token cache ────────────────────────────────────────────────────

    static final class OAuth2TokenCache {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final HttpClient   HTTP   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();

        private static final ConcurrentHashMap<String, CachedToken> CACHE = new ConcurrentHashMap<>();

        static String getToken(String tokenUrl, String clientId, String clientSecret) {
            String key = tokenUrl + "|" + clientId;
            CachedToken cached = CACHE.get(key);
            if (cached != null && !cached.isExpired()) return cached.token;

            CachedToken fresh = fetchToken(tokenUrl, clientId, clientSecret);
            CACHE.put(key, fresh);
            return fresh.token;
        }

        private static CachedToken fetchToken(String tokenUrl, String clientId, String clientSecret) {
            try {
                String form = "grant_type=client_credentials"
                            + "&client_id=" + clientId
                            + "&client_secret=" + clientSecret;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    throw new RuntimeException("[ApiAuth] OAuth2 token request failed: HTTP "
                            + res.statusCode() + " — " + res.body());
                }

                JsonNode json       = MAPPER.readTree(res.body());
                String   token      = json.path("access_token").asText();
                int      expiresIn  = json.path("expires_in").asInt(3600);

                return new CachedToken(token, Instant.now().plusSeconds(expiresIn - 60));

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("[ApiAuth] Failed to fetch OAuth2 token from: " + tokenUrl, e);
            }
        }

        /** Clears cached tokens — useful in tests or after suite. */
        static void clearCache() {
            CACHE.clear();
        }

        private static final class CachedToken {
            final String  token;
            final Instant expiresAt;

            CachedToken(String token, Instant expiresAt) {
                this.token     = token;
                this.expiresAt = expiresAt;
            }

            boolean isExpired() {
                return Instant.now().isAfter(expiresAt);
            }
        }
    }
}
