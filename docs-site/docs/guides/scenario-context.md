---
sidebar_position: 15
---

# Scenario & Suite Context

Selenium Boot provides two built-in context stores for sharing state between steps and tests without static fields or thread-unsafe globals.

---

## `ScenarioContext` — In-Test State

Thread-local store that lives for the duration of a single test. Automatically cleared after each test.

### In `BaseTest` / `BaseApiTest`

```java
public class OrderTest extends BaseApiTest {

    @Test
    public void createAndVerifyOrder() {
        // Step 1 — create order, store ID
        ApiResponse res = ApiClient.post("/api/orders")
                .body(Map.of("productId", 42))
                .send()
                .assertStatus(201);

        ctx().set("orderId", res.json("$.orderId"));

        // Step 2 — use stored ID in next call
        String orderId = ctx().get("orderId");
        ApiClient.get("/api/orders/" + orderId)
                .send()
                .assertStatus(200);
    }
}
```

### Typed retrieval

```java
ctx().set("userId", 42);
int userId = ctx().get("userId", Integer.class);
```

### Check and remove

```java
boolean has = ctx().has("token");
ctx().remove("token");
ctx().clear();   // clear all entries (done automatically after each test)
```

---

## `SuiteContext` — Cross-Test State

Backed by a `ConcurrentHashMap` — survives between tests for the entire suite run. Thread-safe for parallel execution.

Use this when one test creates a resource that later tests need.

```java
public class ApiFlowTest extends BaseApiTest {

    @Test(priority = 1)
    public void createUser() {
        ApiResponse res = ApiClient.post("/api/users")
                .body(Map.of("name", "Alice"))
                .send()
                .assertStatus(201);

        // Store for later tests
        suiteCtx().set("createdUserId", res.json("$.id"));
    }

    @Test(priority = 2, dependsOnMethods = "createUser")
    public void verifyUserExists() {
        String userId = suiteCtx().get("createdUserId");

        ApiClient.get("/api/users/" + userId)
                .send()
                .assertStatus(200)
                .assertJson("$.name", "Alice");
    }

    @Test(priority = 3, dependsOnMethods = "createUser")
    public void deleteUser() {
        String userId = suiteCtx().get("createdUserId");

        ApiClient.delete("/api/users/" + userId)
                .send()
                .assertStatus(204);
    }
}
```

### Methods

```java
suiteCtx().set("key", value);
suiteCtx().get("key");
suiteCtx().get("key", Integer.class);   // typed
suiteCtx().has("key");
suiteCtx().remove("key");
suiteCtx().clear();
```

---

## Hybrid UI + API with Context

Share data between an API call and a browser step in the same test:

```java
public class CheckoutTest extends BaseTest {

    @Test
    public void addItemAndVerifyCart() {
        // API — add item to cart, store cart ID
        ApiResponse cart = apiClient().post("/api/cart/items")
                .body(Map.of("productId", 5, "qty", 2))
                .send()
                .assertStatus(200);

        ctx().set("cartId", cart.json("$.cartId"));

        // UI — open cart and verify
        open("/cart/" + ctx().get("cartId"));
        Assert.assertEquals(getText(By.cssSelector(".item-count")), "2 items");
    }
}
```

---

## When to Use Which

| Need | Use |
|---|---|
| Pass data between steps in one test | `ctx()` — `ScenarioContext` |
| Pass data from test A to test B | `suiteCtx()` — `SuiteContext` |
| Share a created resource ID across the suite | `suiteCtx()` |
| Store a token for one test only | `ctx()` |
| Store a suite-wide auth token | `ApiClient.setGlobalAuth()` |
