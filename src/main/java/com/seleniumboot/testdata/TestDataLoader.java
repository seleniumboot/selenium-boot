package com.seleniumboot.testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Loads test data files from the classpath ({@code testdata/} directory).
 *
 * <p>Supports {@code .json} (Jackson) and {@code .yml} / {@code .yaml} (SnakeYAML).
 *
 * <p>Environment override: when {@code -Denv=staging} is set, the loader first checks
 * for {@code users/admin.staging.json} before falling back to {@code users/admin.json}.
 */
public final class TestDataLoader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BASE_PATH = "testdata/";

    private TestDataLoader() {}

    /**
     * Loads test data for the given relative file path.
     *
     * @param filePath relative path inside {@code testdata/}, e.g. {@code "users/admin.json"}
     * @return parsed data as {@code Map<String, Object>}
     * @throws IllegalArgumentException if the file cannot be found or parsed
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String filePath) {
        String env = System.getProperty("env");

        // Try env-specific file first: "users/admin.json" -> "users/admin.staging.json"
        if (env != null && !env.isEmpty()) {
            String envPath = insertEnv(filePath, env);
            InputStream envStream = stream(envPath);
            if (envStream != null) {
                return parse(envStream, envPath);
            }
        }

        // Fall back to base file
        InputStream base = stream(filePath);
        if (base == null) {
            throw new IllegalArgumentException(
                "[TestData] File not found on classpath: 'testdata/" + filePath + "'. " +
                "Place it in src/test/resources/testdata/."
            );
        }
        return parse(base, filePath);
    }

    /** Inserts env before the extension: "users/admin.json" -> "users/admin.staging.json" */
    private static String insertEnv(String filePath, String env) {
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return filePath + "." + env;
        return filePath.substring(0, dot) + "." + env + filePath.substring(dot);
    }

    private static InputStream stream(String filePath) {
        return TestDataLoader.class.getClassLoader().getResourceAsStream(BASE_PATH + filePath);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(InputStream stream, String filePath) {
        String lower = filePath.toLowerCase();
        try (InputStream is = stream) {
            if (lower.endsWith(".json")) {
                return JSON.readValue(is, new TypeReference<Map<String, Object>>() {});
            } else if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
                Map<String, Object> data = new Yaml().load(is);
                if (data == null) return java.util.Collections.emptyMap();
                return data;
            } else {
                throw new IllegalArgumentException(
                    "[TestData] Unsupported file format: '" + filePath + "'. Use .json, .yml, or .yaml."
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[TestData] Failed to parse: 'testdata/" + filePath + "'", e);
        }
    }
}
