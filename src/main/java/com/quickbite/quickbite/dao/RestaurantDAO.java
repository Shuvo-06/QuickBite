package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Restaurant;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestaurantDAO {

    /** Loads every restaurant together with its menu, using two queries instead of one per restaurant. */
    public List<Restaurant> findAll() throws SQLException {
        // LinkedHashMap keeps the restaurants in the order they came from the database.
        Map<Integer, Restaurant> restaurantsById = new LinkedHashMap<>();

        try (Connection conn = DatabaseManager.getConnection()) {

            String restaurantSql = "SELECT id, name, description, rating FROM restaurants ORDER BY id";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(restaurantSql)) {
                while (rs.next()) {
                    Restaurant restaurant = new Restaurant(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("rating"));
                    restaurantsById.put(restaurant.getId(), restaurant);
                }
            }

            String foodSql = "SELECT id, restaurant_id, name, description, price FROM foods ORDER BY id";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(foodSql)) {
                while (rs.next()) {
                    Restaurant restaurant = restaurantsById.get(rs.getInt("restaurant_id"));
                    if (restaurant != null) {
                        restaurant.addFood(new FoodItem(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getDouble("price")));
                    }
                }
            }
        }

        return new ArrayList<>(restaurantsById.values());
    }
}