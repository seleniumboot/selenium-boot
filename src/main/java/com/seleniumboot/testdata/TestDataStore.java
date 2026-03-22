package com.seleniumboot.testdata;

import java.util.Collections;
import java.util.Map;

/**
 * Thread-local storage for the current test's data loaded by {@link TestDataLoader}.
 * Cleared automatically after each test by the framework.
 */
public final class TestDataStore {

    private static final ThreadLocal<Map<String, Object>> STORE = new ThreadLocal<>();

    private TestDataStore() {}

    public static void set(Map<String, Object> data) {
        STORE.set(Collections.unmodifiableMap(data));
    }

    /** Returns the current test's data, or an empty map if no {@code @TestData} was declared. */
    public static Map<String, Object> get() {
        Map<String, Object> data = STORE.get();
        return data != null ? data : Collections.emptyMap();
    }

    public static void clear() {
        STORE.remove();
    }
}
