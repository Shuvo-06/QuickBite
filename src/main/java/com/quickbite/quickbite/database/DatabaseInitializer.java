package com.quickbite.quickbite.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs once at startup: creates the tables if they do not exist, migrates older database files
 * created by earlier versions of QuickBite so they gain the new columns, and seeds sample data.
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            createTables(conn);
            migrateOlderDatabases(conn);
            seedOnFirstRun(conn);

            System.out.println("Database ready (" + DatabaseManager.DB_FILE + "): "
                    + "restaurants=" + count(conn, "restaurants")
                    + ", foods=" + count(conn, "foods")
                    + ", orders=" + count(conn, "orders")
                    + ", order_items=" + count(conn, "order_items")
                    + ", users=" + count(conn, "users")
                    + ", coupons=" + count(conn, "coupons"));
        }
    }

    // ------------------------------------------------------------------
    // Tables
    // ------------------------------------------------------------------

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS restaurants (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        name        TEXT NOT NULL,
                        description TEXT,
                        rating      REAL NOT NULL DEFAULT 0,
                        is_active   INTEGER NOT NULL DEFAULT 1,
                        image_url   TEXT,
                        password    TEXT NOT NULL DEFAULT 'restaurant123'
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS foods (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        restaurant_id INTEGER NOT NULL,
                        name          TEXT NOT NULL,
                        description   TEXT,
                        price         REAL NOT NULL CHECK (price >= 0),
                        image_url     TEXT,
                        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
                    )
                    """);

            // A real, persistent account table for customers (Phase 11). Restaurant and admin
            // logins remain the lightweight placeholder flows described in the project brief.
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id       INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS coupons (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        code             TEXT NOT NULL UNIQUE,
                        discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 1 AND 100),
                        start_date       TEXT NOT NULL,
                        end_date         TEXT NOT NULL,
                        is_active        INTEGER NOT NULL DEFAULT 1
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_name    TEXT NOT NULL,
                        restaurant_id    INTEGER NOT NULL,
                        restaurant_name  TEXT NOT NULL,
                        total            REAL NOT NULL,
                        status           TEXT NOT NULL,
                        created_at       TEXT NOT NULL,
                        coupon_code      TEXT,
                        discount_amount  REAL NOT NULL DEFAULT 0,
                        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS order_items (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id   INTEGER NOT NULL,
                        food_name  TEXT NOT NULL,
                        quantity   INTEGER NOT NULL CHECK (quantity > 0),
                        unit_price REAL NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                    )
                    """);
        }
    }

    /**
     * "CREATE TABLE IF NOT EXISTS" does nothing to a table that already exists from an earlier
     * version, so a database file created before this update would be missing the new columns.
     * This adds any column that isn't there yet, using SQLite's PRAGMA table_info to check first.
     */
    private static void migrateOlderDatabases(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "restaurants", "is_active", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(conn, "restaurants", "image_url", "TEXT");
        addColumnIfMissing(conn, "foods", "image_url", "TEXT");
        addColumnIfMissing(conn, "orders", "coupon_code", "TEXT");
        addColumnIfMissing(conn, "orders", "discount_amount", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(conn, "restaurants", "password", "TEXT NOT NULL DEFAULT 'restaurant123'");
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) {
                    return; // already has this column, nothing to do
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Migrated database: added " + table + "." + column);
        }
    }

    /**
     * Sample data is inserted only the FIRST time a database is ever set up. PRAGMA user_version
     * remembers that, so after the admin uses "Reset All Data", restarting the app does not bring
     * the sample restaurants back.
     */
    private static void seedOnFirstRun(Connection conn) throws SQLException {
        int version;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            version = rs.next() ? rs.getInt(1) : 0;
        }
        if (version >= 1) {
            return; // already set up once, even if the data has since been reset
        }
        if (count(conn, "restaurants") == 0) {
            insertSampleData(conn);
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = 1");
        }
    }

    private static void insertSampleData(Connection conn) throws SQLException {
        conn.setAutoCommit(false); // all sample rows are saved together, or not at all
        try {
            int pizza = insertRestaurant(conn, "Pizza Palace", "Wood-fired pizzas and Italian favourites.", 4.6, "pizza123");
            insertFood(conn, pizza, "Margherita Pizza", "Tomato sauce, mozzarella and fresh basil.", 250);
            insertFood(conn, pizza, "Chicken Pizza", "Grilled chicken, capsicum and cheese.", 350);
            insertFood(conn, pizza, "Garlic Bread", "Toasted bread with garlic butter.", 120);
            insertFood(conn, pizza, "Cold Drink", "Chilled soft drink, 500 ml.", 60);

            int burger = insertRestaurant(conn, "Burger House", "Juicy burgers and crispy fries.", 4.3, "burger123");
            insertFood(conn, burger, "Chicken Burger", "Crispy chicken fillet with mayo.", 180);
            insertFood(conn, burger, "Beef Burger", "Beef patty, cheddar and lettuce.", 250);
            insertFood(conn, burger, "French Fries", "Golden and lightly salted.", 100);
            insertFood(conn, burger, "Chocolate Shake", "Thick and creamy.", 150);

            int bengal = insertRestaurant(conn, "Bengal Bites", "Traditional Bengali home-style meals.", 4.8, "bengal123");
            insertFood(conn, bengal, "Chicken Biryani", "Fragrant rice with spiced chicken.", 280);
            insertFood(conn, bengal, "Beef Tehari", "Rice cooked with tender beef.", 240);
            insertFood(conn, bengal, "Beef Bhuna", "Slow-cooked beef in rich masala.", 320);
            insertFood(conn, bengal, "Mishti Doi", "Sweet traditional yogurt.", 70);

            int noodle = insertRestaurant(conn, "Noodle Station", "Fresh noodles and Asian street food.", 4.4, "noodle123");
            insertFood(conn, noodle, "Chicken Chow Mein", "Stir-fried noodles with chicken.", 220);
            insertFood(conn, noodle, "Thai Soup", "Hot and sour soup with vegetables.", 200);
            insertFood(conn, noodle, "Vegetable Fried Rice", "Wok-fried rice with fresh vegetables.", 180);
            insertFood(conn, noodle, "Spring Rolls", "Crispy rolls with dipping sauce.", 140);

            insertCoupon(conn, "WELCOME10", 10,
                    java.time.LocalDate.now().minusDays(1),
                    java.time.LocalDate.now().plusMonths(1));

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static int insertRestaurant(Connection conn, String name, String description, double rating,
                                        String password) throws SQLException {
        String sql = "INSERT INTO restaurants (name, description, rating, is_active, image_url, password) "
                + "VALUES (?, ?, ?, 1, NULL, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, rating);
            ps.setString(4, password);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void insertFood(Connection conn, int restaurantId, String name, String description, double price)
            throws SQLException {
        String sql = "INSERT INTO foods (restaurant_id, name, description, price, image_url) "
                + "VALUES (?, ?, ?, ?, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.executeUpdate();
        }
    }

    private static void insertCoupon(Connection conn, String code, int discountPercent,
                                     java.time.LocalDate start, java.time.LocalDate end) throws SQLException {
        String sql = "INSERT INTO coupons (code, discount_percent, start_date, end_date, is_active) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, discountPercent);
            ps.setString(3, start.toString());
            ps.setString(4, end.toString());
            ps.executeUpdate();
        }
    }

    private static int count(Connection conn, String table) throws SQLException {
        // A table name cannot be passed as a '?' parameter, so only hard-coded names
        // from this class are ever used here. No user input reaches this query.
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
