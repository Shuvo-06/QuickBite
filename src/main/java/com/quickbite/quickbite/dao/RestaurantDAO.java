package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Restaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestaurantDAO extends BaseDao<Restaurant> {

    /** Active restaurants only, each with its menu. This is what customers browse. */
    @Override
    public List<Restaurant> findAll() throws SQLException {
        return findRestaurants("WHERE is_active = 1");
    }

    /** Every restaurant regardless of active status, each with its menu. Used by the admin panel. */
    public List<Restaurant> findAllIncludingInactive() throws SQLException {
        return findRestaurants("");
    }

    private List<Restaurant> findRestaurants(String whereClause) throws SQLException {
        // LinkedHashMap keeps the restaurants in the order they came from the database.
        Map<Integer, Restaurant> restaurantsById = new LinkedHashMap<>();

        try (Connection conn = DatabaseManager.getConnection()) {

            String restaurantSql = "SELECT id, name, description, rating, is_active, image_url "
                    + "FROM restaurants " + whereClause + " ORDER BY id";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(restaurantSql)) {
                while (rs.next()) {
                    Restaurant restaurant = new Restaurant(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("rating"),
                            rs.getInt("is_active") == 1,
                            rs.getString("image_url"));
                    restaurantsById.put(restaurant.getId(), restaurant);
                }
            }

            String foodSql = "SELECT id, restaurant_id, name, description, price, image_url FROM foods ORDER BY id";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(foodSql)) {
                while (rs.next()) {
                    Restaurant restaurant = restaurantsById.get(rs.getInt("restaurant_id"));
                    if (restaurant != null) {
                        restaurant.addFood(new FoodItem(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getDouble("price"),
                                rs.getString("image_url")));
                    }
                }
            }
        }

        return new ArrayList<>(restaurantsById.values());
    }

    /** Creates a new restaurant (admin panel) and returns its database-assigned id. */
    public int insert(String name, String description, double rating, String imageUrl) throws SQLException {
        String sql = "INSERT INTO restaurants (name, description, rating, is_active, image_url) VALUES (?, ?, ?, 1, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, rating);
            ps.setString(4, blankToNull(imageUrl));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Updates a restaurant's editable fields (admin panel). Active status is changed via setActive(). */
    public void update(int id, String name, String description, double rating, String imageUrl) throws SQLException {
        String sql = "UPDATE restaurants SET name = ?, description = ?, rating = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, rating);
            ps.setString(4, blankToNull(imageUrl));
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    /** Whitelists (true) or blacklists (false) a restaurant. Blacklisted restaurants stop showing up for customers. */
    public void setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE restaurants SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Deletes a restaurant and (via ON DELETE CASCADE) its foods. Existing past orders are kept for history. */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM restaurants WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
