---
sidebar_position: 14
---

# API Authentication

Selenium Boot supports three auth strategies out of the box: Bearer token, Basic auth, and OAuth2 client credentials.

---

## Bearer Token

```java
ApiClient.get("/api/me")
        .auth(ApiAuth.bearerToken("my-secret-token"))
        .send();
```

---

## Basic Auth

```java
ApiClient.get("/api/admin")
        .auth(ApiAuth.basicAuth("admin", "password"))
        .send();
```

---

## OAuth2 — Client Credentials

Token is fetched automatically on first use and **cached until expiry**. No manual token refresh needed.

```java
ApiClient.setGlobalAuth(ApiAuth.oauth2(
    "https://auth.example.com/token",
    System.getenv("CLIENT_ID"),
    System.getenv("CLIENT_SECRET")
));
```

The framework sends a `POST` with `grant_type=client_credentials` and caches the returned `access_token` until it expires (using the `expires_in` field from the response).

---

## Global Auth — Set Once, Use Everywhere

Set auth once in `@BeforeSuite` and every subsequent request on that thread automatically includes it. No `.auth()` call needed on each request.

```java
import com.seleniumboot.test.BaseApiTest;
import com.seleniumboot.client.ApiAuth;
import com.seleniumboot.client.ApiClient;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class UserApiTest extends BaseApiTest {

    @BeforeSuite
    public void authenticate() {
        ApiResponse login = ApiClient.post("/api/auth/login")
                .body(Map.of("username", "admin", "password", "pass"))
                .send();

        ApiClient.setGlobalAuth(ApiAuth.bearerToken(login.json("$.token")));
    }

    @Test
    public void getUsers() {
        // Token applied automatically — no .auth() needed
        ApiClient.get("/api/users").send().assertStatus(200);
    }
}
```

The framework clears global auth automatically after each test, so tests don't bleed into each other.

To clear it manually:

```java
ApiClient.clearGlobalAuth();
```

---

## `@UseAuth` — Config-Based Auth Strategies

Define named auth strategies in `selenium-boot.yml` and apply them per test or per class with `@UseAuth`.

### Configuration

```yaml
api:
  auth:
    adminToken:
      type: bearer
      token: ${ADMIN_TOKEN}        # resolved from environment variable

    basicUser:
      type: basic
      username: user
      password: ${USER_PASSWORD}

    serviceAccount:
      type: oauth2
      tokenUrl: https://auth.example.com/token
      clientId: ${CLIENT_ID}
      clientSecret: ${CLIENT_SECRET}
```

Token values support `${ENV_VAR}` interpolation — resolved from environment variables or system properties at runtime.

### Usage

```java
@Test
@UseAuth("adminToken")
public void createUser() {
    apiClient().post("/api/users")
            .body(Map.of("name", "Alice"))
            .send()
            .assertStatus(201);
}
```

Apply to an entire class:

```java
@UseAuth("serviceAccount")
public class OrderApiTest extends BaseApiTest {

    @Test
    public void listOrders() {
        ApiClient.get("/api/orders").send().assertStatus(200);
    }

    @Test
    public void createOrder() {
        ApiClient.post("/api/orders").body(...).send().assertStatus(201);
    }
}
```

Method-level `@UseAuth` takes precedence over class-level.

---

## Per-Request vs Global Auth

| Approach | Scope | Best for |
|---|---|---|
| `.auth(ApiAuth.bearerToken(...))` | Single request | One-off calls with different tokens |
| `ApiClient.setGlobalAuth(...)` | All requests on thread | Runtime tokens (login response) |
| `@UseAuth("name")` | Test method or class | Config/env-var based tokens in CI |
