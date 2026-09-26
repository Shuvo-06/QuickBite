package com.quickbite.quickbite.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the SQLite database file.
 * DAOs call getConnection() inside try-with-resources so the connection is always closed.
 */
public class DatabaseManager {

    public static final String DB_FILE = "quickbite.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        // DriverManager finds the SQLite driver on the module path and opens (or creates) the file.
        Connection conn = DriverManager.getConnection(DB_URL);

        try (Statement stmt = conn.createStatement()) {
            // SQLite ignores foreign keys unless this is switched on for EACH connection.
            stmt.execute("PRAGMA foreign_keys = ON");
            // If another thread (e.g. the order-tracker background thread) is writing at the same
            // instant, wait up to 5 seconds instead of failing immediately.
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }
}
