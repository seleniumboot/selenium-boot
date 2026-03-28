---
sidebar_position: 13
---

# API Testing

Selenium Boot supports **pure API tests** and **hybrid UI + API tests** out of the box — same framework, same config, same report.

---

## Pure API Tests — `BaseApiTest`

Extend `BaseApiTest` instead of `BaseTest`. No browser is launched; the full framework lifecycle still applies (reporting, `@TestData`, retry, CI gates).

```java
import com.seleniumboot.test.BaseApiTest;
import com.seleniumboot.client.ApiClient;
import com.seleniumboot.client.ApiResponse;
import org.testng.annotations.Test;

public class UserApiTest extends BaseApiTest {

    @Test
    public void getUserById() {
        ApiResponse res = ApiClient.get("https://api.example.com/users/1")
                .send();

        res.assertStatus(200);
        res.assertJson("$.name", "John Doe");
    }
}
```

---

## `ApiClient` — Fluent HTTP Client

### Supported methods

```java
ApiClient.get("/api/users")
ApiClient.post("/api/users")
ApiClient.put("/api/users/1")
ApiClient.patch("/api/users/1")
ApiClient.delete("/api/users/1")
```

### Base URL

By default, `ApiClient` uses `api.baseUrl` from `selenium-boot.yml`. Falls back to `execution.baseUrl` if not set.

```yaml
api:
  baseUrl: https://api.example.com
  timeoutSeconds: 30
  logBody: false   # set true to log request/response body in step timeline
```

Override per-request with `ApiClient.to(url)`:

```java
ApiClient.to("https://other-service.com").get("/health").send();
```

### Request headers and body

```java
ApiClient.post("/api/users")
        .header("X-Request-ID", "abc123")
        .contentType("application/json")
        .body(Map.of("name", "Alice", "email", "alice@example.com"))
        .send();
```

---

## `ApiResponse` — Assertions and Extraction

### Status assertion

```java
res.assertStatus(201);
```

### Body assertions

```java
res.assertBodyContains("success");
res.assertJson("$.user.name", "Alice");
```

### JSONPath extraction

```java
String token = res.json("$.token");
int    id     = res.json("$.user.id", Integer.class);
```

### Deserialize to object

```java
User user = res.asObject(User.class);
```

### Schema validation

Validate the response structure against a JSON Schema file:

```java
res.assertStatus(200).assertSchema("schemas/user.json");
```

Place schema files under `src/test/resources/schemas/`. See [Schema Validation](#schema-validation) below.

### Raw access

```java
int    status   = res.status();
String body     = res.body();
long   duration = res.durationMs();
```

### Fluent chaining

```java
ApiClient.get("/api/users/1")
        .send()
        .assertStatus(200)
        .assertJson("$.name", "Alice")
        .assertSchema("schemas/user.json");
```

---

## Schema Validation

Validate that a response matches a JSON Schema (Draft-07):

**`src/test/resources/schemas/user.json`**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "name", "email"],
  "properties": {
    "id":    { "type": "integer" },
    "name":  { "type": "string", "minLength": 1 },
    "email": { "type": "string" }
  }
}
```

```java
ApiClient.get("/api/users/1")
        .send()
        .assertStatus(200)
        .assertSchema("schemas/user.json");
```

Requires `com.networknt:json-schema-validator` in your `pom.xml`:

```xml
<dependency>
  <groupId>com.networknt</groupId>
  <artifactId>json-schema-validator</artifactId>
  <version>1.4.3</version>
</dependency>
```

---

## Hybrid UI + API Tests

Mix API calls and browser interactions in the same test. Available in `BaseTest` via `apiClient()`:

```java
public class CheckoutTest extends BaseTest {

    @Test
    public void placeOrder() {
        // Set up order via API (fast)
        ApiResponse order = apiClient().post("/api/orders")
                .body(Map.of("productId", 42, "qty", 1))
                .send()
                .assertStatus(201);

        String orderId = order.json("$.orderId");

        // Verify in the UI
        open("/orders/" + orderId);
        Assert.assertEquals(getText(By.id("status")), "Pending");
    }
}
```

---

## Step Timeline

Every `ApiClient` request is automatically logged in the step timeline:

```
[API] GET /api/users/1 → 200 (143ms)
[API] POST /api/orders → 201 (89ms)
[API] DELETE /api/orders/5 → 404 (12ms)   ← logged as FAIL
```

Enable body logging in `selenium-boot.yml`:

```yaml
api:
  logBody: true
```
