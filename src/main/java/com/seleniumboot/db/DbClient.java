package com.seleniumboot.db;

import com.seleniumboot.api.SeleniumBootApi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * JDBC-backed database assertion client. No ORM required — plain SQL.
 *
 * <p>Obtain via {@code BaseTest}:
 * <pre>
 * // default datasource from selenium-boot.yml database.url
 * db().assertRowExists("users", Map.of("email", "admin@example.com"));
 * db().assertNoRow("orders", Map.of("status", "pending"));
 * db().assertRowCount("products", 5);
 * db().assertRowCount("products", "category = 'electronics'", 3);
 * db().query("SELECT name FROM users WHERE id = ?", 1).assertValue("name", "Alice");
 * String name = (String) db().scalar("SELECT name FROM users WHERE id = ?", 1);
 *
 * // named datasource from database.datasources.reporting
 * db("reporting").assertRowCount("monthly_summary", 12);
 * </pre>
 *
 * <p>Config in {@code selenium-boot.yml}:
 * <pre>
 * database:
 *   url: jdbc:postgresql://localhost/mydb
 *   username: ${DB_USER}
 *   password: ${DB_PASS}
 *   driver: org.postgresql.Driver   # optional; auto-detected from URL by most drivers
 * </pre>
 */
@SeleniumBootApi(since = "1.12.0")
public final class DbClient {

    private final String datasourceName;

    private DbClient(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    /** Returns a client backed by the default {@code database} config block. */
    public static DbClient forDefault() {
        return new DbClient(DbConnectionFactory.DEFAULT);
    }

    /** Returns a client backed by the named entry under {@code database.datasources}. */
    public static DbClient forNamed(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Datasource name must not be blank");
        return new DbClient(name);
    }

    // ── Parameterised query ────────────────────────────────────────────

    /**
     * Returns a {@link DbQuery} for a parameterised SQL statement.
     * Call {@link DbQuery#assertValue} or {@link DbQuery#value} to execute.
     *
     * <pre>
     * db().query("SELECT status FROM orders WHERE id = ?", orderId)
     *     .assertValue("status", "SHIPPED");
     * </pre>
     */
    public DbQuery query(String sql, Object... params) {
        return new DbQuery(datasourceName, sql, params);
    }

    /**
     * Executes a query expected to return a single value and returns it.
     *
     * <pre>
     * long count = (Long) db().scalar("SELECT COUNT(*) FROM orders WHERE user_id = ?", userId);
     * </pre>
     *
     * @throws DbAssertException if the query returns no rows
     */
    public Object scalar(String sql, Object... params) {
        try {
            Connection conn = DbConnectionFactory.getConnection(datasourceName);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                DbQuery.bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DbAssertException("scalar() returned no rows. SQL: " + sql);
                    }
                    return rs.getObject(1);
                }
            }
        } catch (DbAssertException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("[Database] scalar() failed: " + e.getMessage(), e);
        }
    }

    // ── Row existence assertions ───────────────────────────────────────

    /**
     * Asserts that at least one row in {@code table} matches all {@code conditions}.
     *
     * <pre>
     * db().assertRowExists("users", Map.of("email", "alice@example.com", "active", true));
     * </pre>
     *
     * @throws DbAssertException if no matching row is found
     */
    public void assertRowExists(String table, Map<String, Object> conditions) {
        long count = countWhere(table, conditions);
        if (count == 0) {
            throw new DbAssertException(
                "Expected at least one row in '" + table + "' matching " + conditions + " but found none."
            );
        }
    }

    /**
     * Asserts that no row in {@code table} matches the given {@code conditions}.
     *
     * <pre>
     * db().assertNoRow("orders", Map.of("status", "PENDING"));
     * </pre>
     *
     * @throws DbAssertException if a matching row exists
     */
    public void assertNoRow(String table, Map<String, Object> conditions) {
        long count = countWhere(table, conditions);
        if (count > 0) {
            throw new DbAssertException(
                "Expected no rows in '" + table + "' matching " + conditions +
                " but found " + count + "."
            );
        }
    }

    // ── Row count assertions ───────────────────────────────────────────

    /**
     * Asserts that {@code table} contains exactly {@code expected} rows.
     *
     * @throws DbAssertException if the actual count differs
     */
    public void assertRowCount(String table, int expected) {
        long actual = countRaw("SELECT COUNT(*) FROM " + table);
        if (actual != expected) {
            throw new DbAssertException(
                "Expected " + expected + " row(s) in '" + table + "' but found " + actual + "."
            );
        }
    }

    /**
     * Asserts that the number of rows in {@code table} matching the raw {@code where}
     * clause equals {@code expected}.
     *
     * <pre>
     * db().assertRowCount("products", "category = 'electronics'", 3);
     * </pre>
     *
     * @throws DbAssertException if the actual count differs
     */
    public void assertRowCount(String table, String where, int expected) {
        long actual = countRaw("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        if (actual != expected) {
            throw new DbAssertException(
                "Expected " + expected + " row(s) in '" + table +
                "' WHERE " + where + " but found " + actual + "."
            );
        }
    }

    // ── Internals ──────────────────────────────────────────────────────

    private long countWhere(String table, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("[Database] conditions map must not be empty");
        }
        StringJoiner where = new StringJoiner(" AND ");
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> e : conditions.entrySet()) {
            where.add(e.getKey() + " = ?");
            values.add(e.getValue());
        }
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + where;
        try {
            Connection conn = DbConnectionFactory.getConnection(datasourceName);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                DbQuery.bindParams(ps, values.toArray());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        } catch (DbAssertException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("[Database] Count query failed: " + e.getMessage(), e);
        }
    }

    private long countRaw(String sql) {
        try {
            Connection conn = DbConnectionFactory.getConnection(datasourceName);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("[Database] Count query failed: " + e.getMessage(), e);
        }
    }
}
