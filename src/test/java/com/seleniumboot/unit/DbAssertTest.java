package com.seleniumboot.unit;

import com.seleniumboot.db.DbAssertException;
import com.seleniumboot.db.DbClient;
import com.seleniumboot.db.DbConnectionFactory;
import com.seleniumboot.db.DbQuery;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link DbClient}, {@link DbQuery}, and {@link DbAssertException}.
 * Uses mock JDBC objects — no real database required.
 */
public class DbAssertTest {

    // ── DbAssertException ─────────────────────────────────────────────

    @Test
    public void dbAssertException_isAssertionError() {
        DbAssertException ex = new DbAssertException("row not found");
        assertTrue(ex instanceof AssertionError);
        assertEquals(ex.getMessage(), "row not found");
    }

    @Test
    public void dbAssertException_withCause_storesCause() {
        Throwable cause = new RuntimeException("jdbc error");
        DbAssertException ex = new DbAssertException("wrapped", cause);
        assertSame(ex.getCause(), cause);
    }

    // ── DbClient factory ──────────────────────────────────────────────

    @Test
    public void forDefault_returnsNonNull() {
        assertNotNull(DbClient.forDefault());
    }

    @Test
    public void forNamed_returnsNonNull() {
        assertNotNull(DbClient.forNamed("reporting"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void forNamed_blankName_throws() {
        DbClient.forNamed("");
    }

    // ── assertRowExists ───────────────────────────────────────────────

    @Test
    public void assertRowExists_rowFound_passes() throws Exception {
        DbClient db = dbClientWithCountResult(1L);
        db.assertRowExists("users", Map.of("email", "alice@example.com"));
        // No exception — test passes
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void assertRowExists_rowNotFound_throws() throws Exception {
        DbClient db = dbClientWithCountResult(0L);
        db.assertRowExists("users", Map.of("email", "missing@example.com"));
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void assertRowExists_messageContainsTableName() throws Exception {
        try {
            DbClient db = dbClientWithCountResult(0L);
            db.assertRowExists("orders", Map.of("status", "PENDING"));
        } catch (DbAssertException e) {
            assertTrue(e.getMessage().contains("orders"), "Message should contain table name");
            throw e;
        }
    }

    // ── assertNoRow ───────────────────────────────────────────────────

    @Test
    public void assertNoRow_rowAbsent_passes() throws Exception {
        DbClient db = dbClientWithCountResult(0L);
        db.assertNoRow("orders", Map.of("status", "PENDING"));
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void assertNoRow_rowPresent_throws() throws Exception {
        DbClient db = dbClientWithCountResult(2L);
        db.assertNoRow("orders", Map.of("status", "PENDING"));
    }

    // ── assertRowCount ────────────────────────────────────────────────

    @Test
    public void assertRowCount_exact_passes() throws Exception {
        DbClient db = dbClientWithCountResult(5L);
        db.assertRowCount("products", 5);
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void assertRowCount_mismatch_throws() throws Exception {
        DbClient db = dbClientWithCountResult(3L);
        db.assertRowCount("products", 5);
    }

    @Test
    public void assertRowCount_withWhere_passes() throws Exception {
        DbClient db = dbClientWithCountResult(2L);
        db.assertRowCount("products", "category = 'electronics'", 2);
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void assertRowCount_withWhere_mismatch_throws() throws Exception {
        DbClient db = dbClientWithCountResult(0L);
        db.assertRowCount("products", "category = 'electronics'", 2);
    }

    // ── DbQuery.assertValue ───────────────────────────────────────────

    @Test
    public void query_assertValue_match_passes() throws Exception {
        DbClient db = dbClientWithQueryResult("name", "Alice");
        db.query("SELECT name FROM users WHERE id = ?", 1L).assertValue("name", "Alice");
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void query_assertValue_mismatch_throws() throws Exception {
        DbClient db = dbClientWithQueryResult("name", "Bob");
        db.query("SELECT name FROM users WHERE id = ?", 1L).assertValue("name", "Alice");
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void query_assertValue_noRows_throws() throws Exception {
        DbClient db = dbClientWithEmptyResultSet();
        db.query("SELECT name FROM users WHERE id = ?", 999L).assertValue("name", "Alice");
    }

    // ── DbQuery.value ─────────────────────────────────────────────────

    @Test
    public void query_value_returnsColumnValue() throws Exception {
        DbClient db = dbClientWithQueryResult("status", "SHIPPED");
        Object val = db.query("SELECT status FROM orders WHERE id = ?", 42L).value("status");
        assertEquals(String.valueOf(val), "SHIPPED");
    }

    // ── scalar ────────────────────────────────────────────────────────

    @Test
    public void scalar_returnsFirstColumn() throws Exception {
        DbClient db = dbClientWithScalarResult(42L);
        Object result = db.scalar("SELECT COUNT(*) FROM users");
        assertEquals(String.valueOf(result), "42");
    }

    @Test(expectedExceptions = DbAssertException.class)
    public void scalar_noRows_throws() throws Exception {
        DbClient db = dbClientWithEmptyResultSet();
        db.scalar("SELECT name FROM users WHERE id = ?", 999L);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private DbClient dbClientWithCountResult(long count) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(count);

        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rs);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.isClosed()).thenReturn(false);
        when(conn.isValid(anyInt())).thenReturn(true);

        return dbClientWithConnection(conn);
    }

    private DbClient dbClientWithQueryResult(String column, Object value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getObject(column)).thenReturn(value);

        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rs);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.isClosed()).thenReturn(false);
        when(conn.isValid(anyInt())).thenReturn(true);

        return dbClientWithConnection(conn);
    }

    private DbClient dbClientWithScalarResult(Object value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getObject(1)).thenReturn(value);

        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rs);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.isClosed()).thenReturn(false);
        when(conn.isValid(anyInt())).thenReturn(true);

        return dbClientWithConnection(conn);
    }

    private DbClient dbClientWithEmptyResultSet() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);

        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rs);

        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.isClosed()).thenReturn(false);
        when(conn.isValid(anyInt())).thenReturn(true);

        return dbClientWithConnection(conn);
    }

    @SuppressWarnings("unchecked")
    private DbClient dbClientWithConnection(Connection conn) throws Exception {
        String key = "test-" + System.nanoTime();

        Field connectionsField = DbConnectionFactory.class.getDeclaredField("CONNECTIONS");
        connectionsField.setAccessible(true);
        ThreadLocal<Map<String, Connection>> connections =
            (ThreadLocal<Map<String, Connection>>) connectionsField.get(null);
        connections.get().put(key, conn);

        // Create DbClient via reflection targeting the named datasource key
        java.lang.reflect.Constructor<DbClient> ctor =
            DbClient.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(key);
    }
}
