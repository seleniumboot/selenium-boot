package com.seleniumboot.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Represents a parameterised SQL query with fluent assertion support.
 * Obtained via {@link DbClient#query(String, Object...)}.
 *
 * <pre>
 * db().query("SELECT name FROM users WHERE id = ?", 1)
 *     .assertValue("name", "Alice");
 * </pre>
 */
public final class DbQuery {

    private final String datasourceName;
    private final String sql;
    private final Object[] params;

    DbQuery(String datasourceName, String sql, Object[] params) {
        this.datasourceName = datasourceName;
        this.sql = sql;
        this.params = params;
    }

    /**
     * Asserts that the named column in the first result row equals {@code expected}.
     * Comparison is done via {@link String#valueOf} so numeric types compare correctly
     * across JDBC driver implementations.
     *
     * @throws DbAssertException if the column value differs from expected, or if the query returns no rows
     */
    public void assertValue(String column, Object expected) {
        Object actual = value(column);
        if (!Objects.equals(String.valueOf(actual), String.valueOf(expected))) {
            throw new DbAssertException(
                "Expected column '" + column + "' to be [" + expected + "] but was [" + actual + "]"
            );
        }
    }

    /**
     * Returns the value of the named column from the first result row.
     *
     * @throws DbAssertException if the query returns no rows
     */
    public Object value(String column) {
        try {
            Connection conn = DbConnectionFactory.getConnection(datasourceName);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DbAssertException(
                            "Expected a result row but query returned no rows. SQL: " + sql
                        );
                    }
                    return rs.getObject(column);
                }
            }
        } catch (DbAssertException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("[Database] Query execution failed: " + e.getMessage(), e);
        }
    }

    static void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
