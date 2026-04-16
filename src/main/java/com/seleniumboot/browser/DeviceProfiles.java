package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of built-in and custom {@link DeviceProfile} presets for mobile/tablet emulation.
 *
 * <p>Built-in profiles:
 * <ul>
 *   <li>{@code iPhone 14}   — 390×844, scale 3.0, mobile, Safari UA</li>
 *   <li>{@code iPhone SE}   — 375×667, scale 2.0, mobile, Safari UA</li>
 *   <li>{@code Pixel 7}     — 412×915, scale 2.625, mobile, Chrome Android UA</li>
 *   <li>{@code Galaxy S23}  — 360×780, scale 3.0, mobile, Chrome Android UA</li>
 *   <li>{@code iPad}        — 810×1080, scale 2.0, not-mobile, Safari iPad UA</li>
 *   <li>{@code iPad Pro 12} — 1024×1366, scale 2.0, not-mobile, Safari iPad UA</li>
 * </ul>
 *
 * <p>Register a custom profile:
 * <pre>
 * DeviceProfile custom = new DeviceProfile("My Device", 412, 915, 2.625, true,
 *     "Mozilla/5.0 (Linux; Android 13; Pixel 7)...");
 * DeviceProfiles.register("Pixel 7 Custom", custom);
 * </pre>
 *
 * <p>Look up a profile (case-insensitive):
 * <pre>
 * DeviceProfile profile = DeviceProfiles.get("iPhone 14");
 * </pre>
 */
@SeleniumBootApi(since = "1.6.0")
public final class DeviceProfiles {

    private static final Map<String, DeviceProfile> REGISTRY = new LinkedHashMap<>();

    static {
        // ---- iOS ----
        register("iPhone 14", new DeviceProfile(
                "iPhone 14", 390, 844, 3.0, true,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"));

        register("iPhone SE", new DeviceProfile(
                "iPhone SE", 375, 667, 2.0, true,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1"));

        // ---- Android ----
        register("Pixel 7", new DeviceProfile(
                "Pixel 7", 412, 915, 2.625, true,
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"));

        register("Galaxy S23", new DeviceProfile(
                "Galaxy S23", 360, 780, 3.0, true,
                "Mozilla/5.0 (Linux; Android 13; SM-S911B) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"));

        // ---- Tablets ----
        register("iPad", new DeviceProfile(
                "iPad", 810, 1080, 2.0, false,
                "Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"));

        register("iPad Pro 12", new DeviceProfile(
                "iPad Pro 12", 1024, 1366, 2.0, false,
                "Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"));
    }

    private DeviceProfiles() {}

    /**
     * Returns a profile by name (case-insensitive).
     *
     * @throws IllegalArgumentException if the profile is not found
     */
    public static DeviceProfile get(String name) {
        DeviceProfile profile = REGISTRY.get(normalize(name));
        if (profile == null) {
            throw new IllegalArgumentException(
                    "[DeviceProfiles] Unknown device: '" + name +
                    "'. Available: " + REGISTRY.keySet());
        }
        return profile;
    }

    /**
     * Returns {@code true} if a profile with the given name exists (case-insensitive).
     */
    public static boolean contains(String name) {
        return REGISTRY.containsKey(normalize(name));
    }

    /**
     * Registers a custom device profile under the given name (case-insensitive key).
     * Overwrites any existing profile with the same name.
     */
    public static void register(String name, DeviceProfile profile) {
        REGISTRY.put(normalize(name), profile);
    }

    /**
     * Returns an unmodifiable view of all registered profile names (normalised, lower-case).
     */
    public static Set<String> names() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
