package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.util.BulkImportParser;
import com.quickbite.quickbite.util.BulkImportParser.ParsedFood;
import com.quickbite.quickbite.util.BulkImportParser.ParsedRestaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Admin-wide operations that span several tables, each done in a single all-or-nothing transaction. */
public class AdminDAO {

    /**
     * Deletes every order, restaurant, menu item, coupon and customer account, and restarts id
     * numbering. The admin login is NOT in the database, so it is untouched by definition.
     */
    public void resetAllData() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                // Children first, so no foreign key is ever violated.
                stmt.executeUpdate("DELETE FROM order_items");
                stmt.executeUpdate("DELETE FROM orders");
                stmt.executeUpdate("DELETE FROM foods");
                stmt.executeUpdate("DELETE FROM restaurants");
                stmt.executeUpdate("DELETE FROM users");
                stmt.executeUpdate("DELETE FROM coupons");
                stmt.executeUpdate("DELETE FROM sqlite_sequence"); // ids start again at 1
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Saves everything the parser found. Foods that had no RESTAURANT line above them go to
     * targetRestaurantId (may be null if there are none). Returns {restaurantsAdded, foodsAdded}.
     */
    public int[] bulkImport(BulkImportParser.Result parsed, Integer targetRestaurantId) throws SQLException {
        String restaurantSql = "INSERT INTO restaurants (name, description, rating, is_active, image_url, password) "
                + "VALUES (?, ?, ?, 1, ?, ?)";
        String foodSql = "INSERT INTO foods (restaurant_id, name, description, price, image_url) VALUES (?, ?, ?, ?, ?)";

        int restaurantCount = 0;
        int foodCount = 0;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (targetRestaurantId != null) {
                    foodCount += insertFoods(conn, foodSql, targetRestaurantId, parsed.orphanFoods());
                }
                for (ParsedRestaurant restaurant : parsed.restaurants()) {
                    int restaurantId;
                    try (PreparedStatement ps = conn.prepareStatement(restaurantSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, restaurant.name);
                        ps.setString(2, restaurant.description);
                        ps.setDouble(3, restaurant.rating);
                        ps.setString(4, blankToNull(restaurant.image));
                        ps.setString(5, restaurant.password);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();
                            restaurantId = keys.getInt(1);
                        }
                    }
                    restaurantCount++;
                    foodCount += insertFoods(conn, foodSql, restaurantId, restaurant.foods);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return new int[] { restaurantCount, foodCount };
    }

    private int insertFoods(Connection conn, String sql, int restaurantId, List<ParsedFood> foods)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ParsedFood food : foods) {
                ps.setInt(1, restaurantId);
                ps.setString(2, food.name());
                ps.setString(3, food.description());
                ps.setDouble(4, food.price());
                ps.setString(5, blankToNull(food.image()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return foods.size();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}