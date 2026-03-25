package com.seleniumboot.client;

import java.net.http.HttpRequest;
import java.util.Base64;

/**
 * Auth strategies for ApiClient.
 *
 * <pre>
 * apiClient().get("/api/me")
 *            .auth(ApiAuth.bearerToken("my-token"))
 *            .send();
 * </pre>
 */
public abstract class ApiAuth {

    public abstract void apply(HttpRequest.Builder builder);

    /** Sets {@code Authorization: Bearer <token>}. */
    public static ApiAuth bearerToken(String token) {
        return new ApiAuth() {
            @Override
            public void apply(HttpRequest.Builder builder) {
                builder.header("Authorization", "Bearer " + token);
            }
        };
    }

    /** Sets {@code Authorization: Basic <base64(user:pass)>}. */
    public static ApiAuth basicAuth(String username, String password) {
        return new ApiAuth() {
            @Override
            public void apply(HttpRequest.Builder builder) {
                String encoded = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes());
                builder.header("Authorization", "Basic " + encoded);
            }
        };
    }
}
