package com.quickbite.quickbite.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs once at startup: creates the tables if they do not exist,
 * and inserts the sample restaurants and menus the first time.
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            createTables(conn);
            seedIfEmpty(conn);

            System.out.println("Database ready (" + DatabaseManager.DB_FILE + "): "
                    + "restaurants=" + count(conn, "restaurants")
                    + ", foods=" + count(conn, "foods")
                    + ", orders=" + count(conn, "orders")
                    + ", order_items=" + count(conn, "order_items"));
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
                        rating      REAL NOT NULL DEFAULT 0
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS foods (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        restaurant_id INTEGER NOT NULL,
                        name          TEXT NOT NULL,
                        description   TEXT,
                        price         REAL NOT NULL CHECK (price >= 0),
                        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
                    )
                    """);

            // customer_name is plain text for now: there is no real user-account system yet.
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_name   TEXT NOT NULL,
                        restaurant_id   INTEGER NOT NULL,
                        restaurant_name TEXT NOT NULL,
                        total           REAL NOT NULL,
                        status          TEXT NOT NULL,
                        created_at      TEXT NOT NULL,
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

    // ------------------------------------------------------------------
    // Sample data (only inserted when the restaurants table is empty)
    // ------------------------------------------------------------------

    private static void seedIfEmpty(Connection conn) throws SQLException {
        if (count(conn, "restaurants") > 0) {
            return;
        }

        conn.setAutoCommit(false); // all sample rows are saved together, or not at all
        try {
            int pizza = insertRestaurant(conn, "Pizza Palace", "Wood-fired pizzas and Italian favourites.", 4.6);
            insertFood(conn, pizza, "Margherita Pizza", "Tomato sauce, mozzarella and fresh basil.", 250);
            insertFood(conn, pizza, "Chicken Pizza", "Grilled chicken, capsicum and cheese.", 350);
            insertFood(conn, pizza, "Garlic Bread", "Toasted bread with garlic butter.", 120);
            insertFood(conn, pizza, "Cold Drink", "Chilled soft drink, 500 ml.", 60);

            int burger = insertRestaurant(conn, "Burger House", "Juicy burgers and crispy fries.", 4.3);
            insertFood(conn, burger, "Chicken Burger", "Crispy chicken fillet with mayo.", 180);
            insertFood(conn, burger, "Beef Burger", "Beef patty, cheddar and lettuce.", 250);
            insertFood(conn, burger, "French Fries", "Golden and lightly salted.", 100);
            insertFood(conn, burger, "Chocolate Shake", "Thick and creamy.", 150);

            int bengal = insertRestaurant(conn, "Bengal Bites", "Traditional Bengali home-style meals.", 4.8);
            insertFood(conn, bengal, "Chicken Biryani", "Fragrant rice with spiced chicken.", 280);
            insertFood(conn, bengal, "Beef Tehari", "Rice cooked with tender beef.", 240);
            insertFood(conn, bengal, "Beef Bhuna", "Slow-cooked beef in rich masala.", 320);
            insertFood(conn, bengal, "Mishti Doi", "Sweet traditional yogurt.", 70);

            int noodle = insertRestaurant(conn, "Noodle Station", "Fresh noodles and Asian street food.", 4.4);
            insertFood(conn, noodle, "Chicken Chow Mein", "Stir-fried noodles with chicken.", 220);
            insertFood(conn, noodle, "Thai Soup", "Hot and sour soup with vegetables.", 200);
            insertFood(conn, noodle, "Vegetable Fried Rice", "Wok-fried rice with fresh vegetables.", 180);
            insertFood(conn, noodle, "Spring Rolls", "Crispy rolls with dipping sauce.", 140);

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static int insertRestaurant(Connection conn, String name, String description, double rating)
            throws SQLException {
        String sql = "INSERT INTO restaurants (name, description, rating) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, rating);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void insertFood(Connection conn, int restaurantId, String name, String description, double price)
            throws SQLException {
        String sql = "INSERT INTO foods (restaurant_id, name, description, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setDouble(4, price);
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