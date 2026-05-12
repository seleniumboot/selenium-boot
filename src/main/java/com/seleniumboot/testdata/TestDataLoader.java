package com.seleniumboot.testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads test data from multiple source types, selected by the prefix in the value string:
 *
 * <ul>
 *   <li>No prefix — JSON ({@code .json}) or YAML ({@code .yml}/{@code .yaml}) file from
 *       {@code src/test/resources/testdata/}.</li>
 *   <li>{@code csv:path} — CSV file from the classpath; header row = column names.</li>
 *   <li>{@code excel:path} — XLSX file via Apache POI (optional dep); use {@code sheet} param.</li>
 *   <li>{@code db:SQL} — executes SQL against the default datasource in {@code selenium-boot.yml}.</li>
 * </ul>
 */
public final class TestDataLoader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BASE_PATH = "testdata/";

    private TestDataLoader() {}

    /**
     * Loads test data using just the source path / query (sheet = first, row = 0).
     * Maintains backward compatibility with the original single-argument API.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String source) {
        return load(source, "", 0);
    }

    /**
     * Loads test data with explicit sheet and row selection.
     *
     * @param source source string — may include a {@code csv:}, {@code excel:}, or {@code db:} prefix
     * @param sheet  sheet name for Excel sources; ignored otherwise
     * @param row    zero-based data-row index (after header) for CSV/Excel; ignored for JSON/YAML/DB
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String source, String sheet, int row) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("[TestData] source must not be empty.");
        }

        if (source.startsWith("csv:")) {
            return loadCsv(source.substring(4), row);
        }
        if (source.startsWith("excel:")) {
            return loadExcel(source.substring(6), sheet, row);
        }
        if (source.startsWith("db:")) {
            return loadDb(source.substring(3));
        }

        // ── JSON / YAML (original behaviour) ─────────────────────────────
        String env = System.getProperty("env");
        if (env != null && !env.isEmpty()) {
            String envPath = insertEnv(source, env);
            InputStream envStream = stream(envPath);
            if (envStream != null) return parseJsonYaml(envStream, envPath);
        }
        InputStream base = stream(source);
        if (base == null) {
            throw new IllegalArgumentException(
                "[TestData] File not found on classpath: 'testdata/" + source + "'. " +
                "Place it in src/test/resources/testdata/."
            );
        }
        return parseJsonYaml(base, source);
    }

    // ── CSV ───────────────────────────────────────────────────────────────

    private static Map<String, Object> loadCsv(String path, int rowIndex) {
        InputStream is = streamDirect(path);
        if (is == null) {
            // try under testdata/ prefix as well
            is = stream(path);
        }
        if (is == null) {
            throw new IllegalArgumentException(
                "[TestData] CSV file not found on classpath: '" + path + "'."
            );
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("[TestData] CSV file is empty: '" + path + "'.");
            }
            List<String> headers = parseCsvLine(headerLine);

            String dataLine = null;
            for (int i = 0; i <= rowIndex; i++) {
                dataLine = reader.readLine();
                if (dataLine == null) {
                    throw new IllegalArgumentException(
                        "[TestData] CSV row " + rowIndex + " not found in '" + path + "' " +
                        "(file has fewer data rows)."
                    );
                }
            }

            List<String> values = parseCsvLine(dataLine);
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                result.put(headers.get(i).trim(), i < values.size() ? coerce(values.get(i).trim()) : "");
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[TestData] Failed to parse CSV: '" + path + "'", e);
        }
    }

    /** Parses one CSV line respecting RFC 4180 quoting (double-quoted fields, escaped "" inside). */
    public static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields;
    }

    // ── Excel (Apache POI — optional dep) ────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadExcel(String path, String sheetName, int rowIndex) {
        InputStream is = streamDirect(path);
        if (is == null) is = stream(path);
        if (is == null) {
            throw new IllegalArgumentException(
                "[TestData] Excel file not found on classpath: '" + path + "'."
            );
        }
        try {
            return ExcelDataReader.read(is, sheetName, rowIndex);
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException(
                "[TestData] Apache POI is required for Excel sources. " +
                "Add 'org.apache.poi:poi-ooxml:5.2.5' to your pom.xml.", e
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[TestData] Failed to parse Excel: '" + path + "'", e);
        }
    }

    // ── Database ──────────────────────────────────────────────────────────

    private static Map<String, Object> loadDb(String sql) {
        Connection conn;
        try {
            conn = com.seleniumboot.db.DbConnectionFactory.getConnection(
                com.seleniumboot.db.DbConnectionFactory.DEFAULT);
        } catch (Exception e) {
            throw new IllegalStateException(
                "[TestData] DB source requires 'database.url' in selenium-boot.yml. " + e.getMessage(), e
            );
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalArgumentException(
                    "[TestData] DB query returned no rows: " + sql
                );
            }
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                result.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[TestData] DB query failed: " + sql, e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Tries to coerce a raw string to int or boolean; falls back to String. */
    public static Object coerce(String value) {
        if ("true".equalsIgnoreCase(value))  return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
        return value;
    }

    /** Inserts env before the extension: "users/admin.json" -> "users/admin.staging.json" */
    private static String insertEnv(String filePath, String env) {
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return filePath + "." + env;
        return filePath.substring(0, dot) + "." + env + filePath.substring(dot);
    }

    /** Resolves path relative to testdata/ on the classpath. */
    private static InputStream stream(String filePath) {
        return TestDataLoader.class.getClassLoader().getResourceAsStream(BASE_PATH + filePath);
    }

    /** Resolves path directly from the classpath root (for csv:/excel: paths that include testdata/). */
    private static InputStream streamDirect(String filePath) {
        return TestDataLoader.class.getClassLoader().getResourceAsStream(filePath);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonYaml(InputStream stream, String filePath) {
        String lower = filePath.toLowerCase();
        try (InputStream is = stream) {
            if (lower.endsWith(".json")) {
                return JSON.readValue(is, new TypeReference<Map<String, Object>>() {});
            } else if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
                Map<String, Object> data = new Yaml().load(is);
                if (data == null) return Collections.emptyMap();
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
