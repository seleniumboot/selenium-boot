package com.seleniumboot.db;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates and caches per-thread JDBC connections.
 * Uses plain {@link java.sql.DriverManager} — no extra dependency required.
 * The consumer's JDBC driver jar (e.g. {@code postgresql}, {@code mysql-connector-j})
 * must be on the classpath.
 *
 * <p>One connection per datasource name per thread is maintained and reused
 * across assertions in the same test. All connections are closed at test end
 * via {@link #closeAll()}.
 */
public final class DbConnectionFactory {

    static final String DEFAULT = "__default__";

    private static final ThreadLocal<Map<String, Connection>> CONNECTIONS =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private DbConnectionFactory() {}

    public static Connection getConnection(String datasourceName) {
        Map<String, Connection> map = CONNECTIONS.get();
        Connection conn = map.get(datasourceName);
        if (conn == null || !isAlive(conn)) {
            conn = createConnection(datasourceName);
            map.put(datasourceName, conn);
        }
        return conn;
    }

    public static void closeAll() {
        Map<String, Connection> map = CONNECTIONS.get();
        for (Connection conn : map.values()) {
            try { conn.close(); } catch (Exception ignored) {}
        }
        map.clear();
        CONNECTIONS.remove();
    }

    private static boolean isAlive(Connection conn) {
        try {
            return !conn.isClosed() && conn.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    private static Connection createConnection(String name) {
        SeleniumBootConfig.Database config = resolveConfig(name);
        if (config == null || config.getUrl() == null || config.getUrl().isEmpty()) {
            throw new IllegalStateException(
                "[Database] No database configuration found" +
                (DEFAULT.equals(name) ? "" : " for datasource '" + name + "'") +
                ". Add 'database.url' to selenium-boot.yml."
            );
        }

        String url      = resolveEnv(config.getUrl());
        String username = resolveEnv(config.getUsername());
        String password = resolveEnv(config.getPassword());
        String driver   = config.getDriver();

        try {
            if (driver != null && !driver.isEmpty()) {
                Class.forName(driver);
            }
            return java.sql.DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("[Database] JDBC driver class not found: " + driver, e);
        } catch (SQLException e) {
            throw new IllegalStateException(
                "[Database] Connection failed" +
                (DEFAULT.equals(name) ? "" : " for '" + name + "'") +
                ": " + e.getMessage(), e);
        }
    }

    private static SeleniumBootConfig.Database resolveConfig(String name) {
        try {
            SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
            if (cfg == null) return null;
            SeleniumBootConfig.Database db = cfg.getDatabase();
            if (db == null) return null;

            if (DEFAULT.equals(name)) return db;

            Map<String, SeleniumBootConfig.Database.DataSource> datasources = db.getDatasources();
            if (datasources == null) return null;
            SeleniumBootConfig.Database.DataSource ds = datasources.get(name);
            if (ds == null) return null;

            SeleniumBootConfig.Database wrapped = new SeleniumBootConfig.Database();
            wrapped.setUrl(ds.getUrl());
            wrapped.setUsername(ds.getUsername());
            wrapped.setPassword(ds.getPassword());
            wrapped.setDriver(ds.getDriver());
            return wrapped;
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveEnv(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String varName = value.substring(2, value.length() - 1);
            String resolved = System.getenv(varName);
            if (resolved == null) resolved = System.getProperty(varName);
            return resolved != null ? resolved : value;
        }
        return value;
    }
}
