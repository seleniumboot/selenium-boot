package com.seleniumboot.testdata;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares test data to inject for a test method or class.
 *
 * <p>Supported source formats:
 * <ul>
 *   <li><b>JSON / YAML</b> (default) — {@code @TestData("users/admin.json")} — file in
 *       {@code src/test/resources/testdata/}.</li>
 *   <li><b>CSV</b> — {@code @TestData("csv:testdata/users.csv")} — first data row loaded by default;
 *       use {@code row} to pick a specific row (0-based, header excluded).</li>
 *   <li><b>Excel</b> — {@code @TestData(value = "excel:testdata/users.xlsx", sheet = "Login")} —
 *       requires {@code org.apache.poi:poi-ooxml} on the classpath.</li>
 *   <li><b>Database</b> — {@code @TestData("db:SELECT username, password FROM test_users WHERE active=1")} —
 *       uses the {@code database} block in {@code selenium-boot.yml}.</li>
 * </ul>
 *
 * <p>Environment-specific override: if {@code -Denv=staging} is set and
 * {@code users/admin.staging.json} exists, it is used instead of {@code users/admin.json}
 * (applies to JSON/YAML only).
 *
 * <p>Class-level annotation acts as default for all methods in the class;
 * a method-level annotation overrides the class-level one.
 *
 * <pre>
 * &#64;Test
 * &#64;TestData("users/admin.json")
 * public void adminCanLogin() {
 *     String username = (String) getTestData().get("username");
 * }
 *
 * &#64;Test
 * &#64;TestData("csv:testdata/logins.csv")
 * public void loginWithCsvRow() {
 *     String username = (String) getTestData().get("username");
 * }
 *
 * &#64;Test
 * &#64;TestData(value = "excel:testdata/users.xlsx", sheet = "Admin", row = 1)
 * public void loginWithExcelRow() { ... }
 *
 * &#64;Test
 * &#64;TestData("db:SELECT username, password FROM test_users LIMIT 1")
 * public void loginFromDatabase() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SeleniumBootApi(since = "1.0.0")
public @interface TestData {

    /**
     * Data source. Supports prefixes {@code csv:}, {@code excel:}, {@code db:}.
     * No prefix = JSON/YAML file path relative to {@code src/test/resources/testdata/}.
     */
    String value();

    /**
     * Sheet name for Excel sources. Ignored for other source types.
     * Defaults to the first sheet when empty.
     */
    String sheet() default "";

    /**
     * Zero-based data-row index (0 = first row after the header).
     * Applies to CSV and Excel sources. Ignored for JSON/YAML and DB.
     */
    int row() default 0;
}
