package com.seleniumboot.extension;

import java.io.InputStream;
import java.util.Properties;

/**
 * Exposes the current Selenium Boot framework version.
 *
 * <p>Plugins can use this to assert compatibility at load time:
 * <pre>
 * public void onLoad(SeleniumBootConfig config) {
 *     FrameworkVersion.requireAtLeast("0.7.0");
 * }
 * </pre>
 */
public final class FrameworkVersion {

    private static final String VERSION = loadVersion();

    private FrameworkVersion() {}

    /** Returns the current framework version string, e.g. {@code "0.7.0"}. */
    public static String get() {
        return VERSION;
    }

    /**
     * Throws {@link IncompatiblePluginException} if the running framework version
     * is older than {@code requiredVersion}.
     *
     * @param requiredVersion minimum version required, e.g. {@code "0.7.0"}
     */
    public static void requireAtLeast(String requiredVersion) {
        if (isOlderThan(VERSION, requiredVersion)) {
            throw new IncompatiblePluginException(
                "This plugin requires Selenium Boot >= " + requiredVersion +
                " but the running version is " + VERSION
            );
        }
    }

    /** Returns true if {@code a} is older than {@code b} using simple numeric comparison. */
    static boolean isOlderThan(String a, String b) {
        int[] partsA = parse(a);
        int[] partsB = parse(b);
        for (int i = 0; i < Math.min(partsA.length, partsB.length); i++) {
            if (partsA[i] < partsB[i]) return true;
            if (partsA[i] > partsB[i]) return false;
        }
        return partsA.length < partsB.length;
    }

    private static int[] parse(String version) {
        String[] parts = version.replaceAll("[^0-9.]", "").split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { nums[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException e) { nums[i] = 0; }
        }
        return nums;
    }

    private static String loadVersion() {
        try (InputStream in = FrameworkVersion.class.getResourceAsStream(
                "/META-INF/maven/io.github.seleniumboot/selenium-boot/pom.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String v = props.getProperty("version");
                if (v != null && !v.isBlank()) return v.trim();
            }
        } catch (Exception ignored) {}
        return "0.0.0";
    }
}
