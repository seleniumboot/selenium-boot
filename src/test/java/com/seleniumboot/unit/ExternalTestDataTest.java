package com.seleniumboot.unit;

import com.seleniumboot.testdata.TestData;
import com.seleniumboot.testdata.TestDataLoader;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link TestDataLoader} — CSV, DB, and annotation attribute support.
 * No real database or browser required.
 */
public class ExternalTestDataTest {

    // ── CSV loading ───────────────────────────────────────────────────────

    @Test
    public void csv_loadsFirstDataRow_byDefault() {
        Map<String, Object> data = TestDataLoader.load("csv:testdata/logins.csv");
        assertEquals(data.get("username"), "admin");
        assertEquals(data.get("password"), "secret");
        assertEquals(data.get("role"), "ADMIN");
    }

    @Test
    public void csv_loadsSecondDataRow_whenRowIs1() {
        Map<String, Object> data = TestDataLoader.load("csv:testdata/logins.csv", "", 1);
        assertEquals(data.get("username"), "user1");
        assertEquals(data.get("password"), "pass1");
    }

    @Test
    public void csv_loadsThirdDataRow_whenRowIs2() {
        Map<String, Object> data = TestDataLoader.load("csv:testdata/logins.csv", "", 2);
        assertEquals(data.get("username"), "user2");
    }

    @Test
    public void csv_coercesBoolean_true() {
        Map<String, Object> data = TestDataLoader.load("csv:testdata/logins.csv");
        assertEquals(data.get("active"), Boolean.TRUE);
    }

    @Test
    public void csv_coercesBoolean_false() {
        Map<String, Object> data = TestDataLoader.load("csv:testdata/logins.csv", "", 2);
        assertEquals(data.get("active"), Boolean.FALSE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*CSV row 99 not found.*")
    public void csv_throwsWhenRowOutOfBounds() {
        TestDataLoader.load("csv:testdata/logins.csv", "", 99);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*CSV file not found.*")
    public void csv_throwsWhenFileNotFound() {
        TestDataLoader.load("csv:testdata/nonexistent.csv");
    }

    // ── parseCsvLine edge cases ───────────────────────────────────────────

    @Test
    public void parseCsvLine_simpleFields() {
        List<String> fields = TestDataLoader.parseCsvLine("a,b,c");
        assertEquals(fields.size(), 3);
        assertEquals(fields.get(0), "a");
        assertEquals(fields.get(2), "c");
    }

    @Test
    public void parseCsvLine_quotedField_preservesComma() {
        List<String> fields = TestDataLoader.parseCsvLine("\"hello, world\",b");
        assertEquals(fields.size(), 2);
        assertEquals(fields.get(0), "hello, world");
    }

    @Test
    public void parseCsvLine_doubleQuoteEscape() {
        List<String> fields = TestDataLoader.parseCsvLine("\"say \"\"hi\"\"\",b");
        assertEquals(fields.get(0), "say \"hi\"");
    }

    @Test
    public void parseCsvLine_emptyFields() {
        List<String> fields = TestDataLoader.parseCsvLine(",b,");
        assertEquals(fields.size(), 3);
        assertEquals(fields.get(0), "");
        assertEquals(fields.get(2), "");
    }

    // ── coerce helper ─────────────────────────────────────────────────────

    @Test
    public void coerce_integer() {
        assertEquals(TestDataLoader.coerce("42"), 42);
    }

    @Test
    public void coerce_double() {
        assertEquals(TestDataLoader.coerce("3.14"), 3.14);
    }

    @Test
    public void coerce_trueBoolean() {
        assertEquals(TestDataLoader.coerce("true"), Boolean.TRUE);
        assertEquals(TestDataLoader.coerce("TRUE"), Boolean.TRUE);
    }

    @Test
    public void coerce_falseBoolean() {
        assertEquals(TestDataLoader.coerce("false"), Boolean.FALSE);
    }

    @Test
    public void coerce_string_unchanged() {
        assertEquals(TestDataLoader.coerce("hello"), "hello");
    }

    @Test
    public void coerce_emptyString_unchanged() {
        assertEquals(TestDataLoader.coerce(""), "");
    }

    // ── @TestData annotation attributes ──────────────────────────────────

    @Test
    public void annotation_defaultAttributes_areBackwardCompatible() throws Exception {
        TestData annotation = AnnotatedMethod.class
            .getMethod("jsonMethod").getAnnotation(TestData.class);
        assertEquals(annotation.value(), "users/admin.json");
        assertEquals(annotation.sheet(), "");
        assertEquals(annotation.row(), 0);
    }

    @Test
    public void annotation_sheetAndRow_areSetCorrectly() throws Exception {
        TestData annotation = AnnotatedMethod.class
            .getMethod("excelMethod").getAnnotation(TestData.class);
        assertEquals(annotation.value(), "excel:testdata/users.xlsx");
        assertEquals(annotation.sheet(), "Admin");
        assertEquals(annotation.row(), 1);
    }

    @Test
    public void annotation_csvPrefix_isDetected() throws Exception {
        TestData annotation = AnnotatedMethod.class
            .getMethod("csvMethod").getAnnotation(TestData.class);
        assertTrue(annotation.value().startsWith("csv:"));
        assertEquals(annotation.row(), 0);
    }

    @Test
    public void annotation_dbPrefix_isDetected() throws Exception {
        TestData annotation = AnnotatedMethod.class
            .getMethod("dbMethod").getAnnotation(TestData.class);
        assertTrue(annotation.value().startsWith("db:"));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Target class for annotation reflection tests. */
    @SuppressWarnings("unused")
    private static final class AnnotatedMethod {
        @TestData("users/admin.json")
        public void jsonMethod() {}

        @TestData(value = "excel:testdata/users.xlsx", sheet = "Admin", row = 1)
        public void excelMethod() {}

        @TestData("csv:testdata/logins.csv")
        public void csvMethod() {}

        @TestData("db:SELECT username FROM test_users LIMIT 1")
        public void dbMethod() {}
    }
}
