package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.FoodItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Lets the admin panel add, edit, and remove menu items for a specific restaurant. */
public class FoodDAO {

    public List<FoodItem> findByRestaurant(int restaurantId) throws SQLException {
        String sql = "SELECT id, name, description, price, image_url FROM foods WHERE restaurant_id = ? ORDER BY id";
        List<FoodItem> foods = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    foods.add(new FoodItem(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getString("image_url")));
                }
            }
        }
        return foods;
    }

    public void insert(int restaurantId, String name, String description, double price, String imageUrl)
            throws SQLException {
        String sql = "INSERT INTO foods (restaurant_id, name, description, price, image_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.setString(5, blankToNull(imageUrl));
            ps.executeUpdate();
        }
    }

    public void update(int foodId, String name, String description, double price, String imageUrl)
            throws SQLException {
        String sql = "UPDATE foods SET name = ?, description = ?, price = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setString(4, blankToNull(imageUrl));
            ps.setInt(5, foodId);
            ps.executeUpdate();
        }
    }

    public void delete(int foodId) throws SQLException {
        String sql = "DELETE FROM foods WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            ps.executeUpdate();
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
