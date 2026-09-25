package com.quickbite.quickbite.dao;

import com.quickbite.quickbite.database.DatabaseManager;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.model.Restaurant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDAO {

    /** Saves a new order and its items in one transaction, then returns the order with its database-assigned id. */
    public Order insertOrder(String customerName, Restaurant restaurant, List<OrderItem> items) throws SQLException {
        String orderSql = "INSERT INTO orders (customer_name, restaurant_id, restaurant_name, total, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, food_name, quantity, unit_price) VALUES (?, ?, ?, ?)";

        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // the order row and its items are saved together, or not at all
            try {
                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, customerName);
                    ps.setInt(2, restaurant.getId());
                    ps.setString(3, restaurant.getName());
                    ps.setDouble(4, total);
                    ps.setString(5, OrderStatus.PLACED.name());
                    ps.setString(6, LocalDateTime.now().toString());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (OrderItem item : items) {
                        ps.setInt(1, orderId);
                        ps.setString(2, item.getFoodName());
                        ps.setInt(3, item.getQuantity());
                        ps.setDouble(4, item.getUnitPrice());
                        ps.addBatch(); // one round trip for all items instead of one per item
                    }
                    ps.executeBatch();
                }

                conn.commit();

                Order order = new Order(orderId, customerName, restaurant.getName());
                for (OrderItem item : items) {
                    order.addItem(item);
                }
                return order;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Persists a status change. Called from a BACKGROUND thread by OrderTrackingService. */
    public void updateStatus(int orderId, OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }
}