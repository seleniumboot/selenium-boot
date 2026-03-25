package com.seleniumboot.context;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global thread-safe store scoped to the entire suite run.
 * Survives between tests — use to share state across test methods.
 *
 * <pre>
 * // Test 1 — store created resource
 * suiteCtx().set("orderId", response.json("$.id"));
 *
 * // Test 2 — retrieve it
 * String orderId = suiteCtx().get("orderId");
 * </pre>
 */
public class SuiteContext {

    private static final ConcurrentHashMap<String, Object> STORE = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        STORE.put(key, value);
    }

    public String get(String key) {
        return get(key, String.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = STORE.get(key);
        if (value == null) {
            throw new IllegalStateException(
                "[SuiteContext] Key not found: '" + key + "'. " +
                "Available keys: " + STORE.keySet());
        }
        return (T) value;
    }

    public boolean has(String key) {
        return STORE.containsKey(key);
    }

    public void remove(String key) {
        STORE.remove(key);
    }

    public Set<String> keys() {
        return STORE.keySet();
    }

    /** Called by the framework at suite end. */
    public static void clear() {
        STORE.clear();
    }
}
