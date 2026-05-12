---
id: external-test-data
title: External Test Data Sources
sidebar_position: 13
---

# External Test Data Sources

Selenium Boot 2.2.0 extends `@TestData` to load rows directly from CSV files, Excel workbooks, and live database queries — no extra boilerplate needed.

---

## Overview

| Prefix | Source | Extra dependency? |
|--------|--------|-------------------|
| *(none)* | JSON / YAML file in `testdata/` | — |
| `csv:` | CSV file from the classpath | — |
| `excel:` | XLSX workbook via Apache POI | `poi-ooxml` |
| `db:` | JDBC query — first result row | JDBC driver for your DB |

---

## CSV

```java
@Test
@TestData("csv:testdata/logins.csv")
public void loginWithCsvData() {
    Map<String, Object> data = getTestData();
    String username = (String) data.get("username");
    String password = (String) data.get("password");
    new LoginPage(getDriver()).login(username, password);
    assertTrue(new DashboardPage(getDriver()).isLoaded());
}
```

The first row of the file is treated as the column header. By default the first data row is loaded. Use `row` to pick a specific row (zero-based, header excluded):

```java
@TestData(value = "csv:testdata/logins.csv", row = 2)  // third data row
public void loginAsThirdUser() { ... }
```

**Type coercion** is applied automatically:

| Cell value | Java type |
|---|---|
| `42` | `Integer` |
| `3.14` | `Double` |
| `true` / `false` | `Boolean` |
| anything else | `String` |

### CSV format

Standard RFC 4180: comma-separated, double-quote delimited, `""` inside a quoted field is an escaped quote.

```
username,password,role,active
admin,secret,ADMIN,true
user1,"pass,1",USER,false
```

No extra dependency — the CSV parser is built into the framework.

---

## Excel (XLSX)

```java
@Test
@TestData(value = "excel:testdata/users.xlsx", sheet = "Login")
public void loginWithExcelData() {
    Map<String, Object> data = getTestData();
    String username = (String) data.get("username");
    // ...
}
```

- `sheet` — sheet name; defaults to the first sheet when omitted
- `row` — zero-based data row (0 = first row after the header row)

```java
@TestData(value = "excel:testdata/users.xlsx", sheet = "Admin", row = 1)
public void loginAsSecondAdminUser() { ... }
```

### Cell type mapping

| Excel type | Java type |
|---|---|
| Numeric (integer) | `long` |
| Numeric (decimal) | `double` |
| Numeric (date-formatted) | `String` (ISO date, e.g. `"2024-03-15"`) |
| Boolean | `Boolean` |
| String | `String` |
| Blank | `""` |

### Required dependency

Add Apache POI to your project's `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
    <scope>test</scope>
</dependency>
```

Without it, you will get a clear error at runtime:

```
[TestData] Apache POI is required for Excel sources.
Add 'org.apache.poi:poi-ooxml:5.2.5' to your pom.xml.
```

---

## Database

Executes a JDBC query against the `database` config block and loads the first result row:

```java
@Test
@TestData("db:SELECT username, password FROM test_users WHERE active = true LIMIT 1")
public void loginWithDbUser() {
    Map<String, Object> data = getTestData();
    String username = (String) data.get("username");
    // ...
}
```

Column labels become map keys. Types come directly from the JDBC `ResultSet` — numbers are already `Integer` / `Long` / `Double`, booleans are `Boolean`, dates are `java.sql.Date`.

### Config required

```yaml
database:
  url:      jdbc:postgresql://localhost/mydb
  username: ${DB_USER}
  password: ${DB_PASS}
```

The DB source reuses the same connection managed by `DbConnectionFactory` — it participates in the same per-test lifecycle and is closed automatically at test end.

---

## File placement

CSV and Excel files are resolved from the **classpath root** first, then under `testdata/` as a fallback. The recommended placement is `src/test/resources/testdata/`:

```
src/test/resources/
  testdata/
    logins.csv
    users.xlsx
    admin.json
```

---

## Backward compatibility

All existing `@TestData("users/admin.json")` usage is unchanged. The new attributes `sheet` and `row` default to `""` and `0` respectively, so existing annotations require no modification.

---

## Combining with environment profiles

Environment override applies to JSON/YAML sources only. CSV, Excel, and DB sources do not support the `-Denv=` override — use `row` to select a specific data row per environment instead.

---

## Class-level annotation

The annotation works at the class level too — all test methods in the class share the same data source:

```java
@TestData("csv:testdata/logins.csv")
public class LoginTests extends BaseTest {

    @Test
    public void loginSucceeds() { ... }

    @Test
    @TestData(value = "csv:testdata/logins.csv", row = 1)  // overrides class-level row
    public void loginWithSecondUser() { ... }
}
```
