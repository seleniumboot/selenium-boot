package com.seleniumboot.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local key-value store scoped to a single test.
 * Auto-cleared after each test by the framework.
 *
 * <pre>
 * ctx().set("authToken", response.json("$.token"));
 * String token = ctx().get("authToken");
 * int id = ctx().get("userId", Integer.class);
 * </pre>
 */
public class ScenarioContext {

    private static final ThreadLocal<Map<String, Object>> STORE =
            ThreadLocal.withInitial(HashMap::new);

    public void set(String key, Object value) {
        STORE.get().put(key, value);
    }

    public String get(String key) {
        return get(key, String.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = STORE.get().get(key);
        if (value == null) {
            throw new IllegalStateException(
                "[ScenarioContext] Key not found: '" + key + "'. " +
                "Available keys: " + STORE.get().keySet());
        }
        return (T) value;
    }

    public boolean has(String key) {
        return STORE.get().containsKey(key);
    }

    public void remove(String key) {
        STORE.get().remove(key);
    }

    /** Called by the framework after each test. */
    public static void clear() {
        STORE.remove();
    }
}
