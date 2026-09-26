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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    /** Saves a new order and its items in one transaction, then returns the order with its database-assigned id. */
    public Order insertOrder(String customerName, Restaurant restaurant, List<OrderItem> items) throws SQLException {
        String orderSql = "INSERT INTO orders (customer_name, restaurant_id, restaurant_name, total, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, food_name, quantity, unit_price) VALUES (?, ?, ?, ?)";

        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        LocalDateTime now = LocalDateTime.now();

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
                    ps.setString(6, now.toString());
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

                Order order = new Order(orderId, customerName, restaurant.getId(), restaurant.getName());
                for (OrderItem item : items) {
                    order.addItem(item);
                }
                order.setCreatedAt(now.format(DISPLAY_FORMAT));
                return order;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Persists a status change. Called from a background thread by OrderTrackingService, and from the dashboard. */
    public void updateStatus(int orderId, OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /** Loads every order placed at one restaurant, most recent first, each with its items. Used by the dashboard. */
    public List<Order> findByRestaurant(int restaurantId) throws SQLException {
        String orderSql = "SELECT id, customer_name, restaurant_id, restaurant_name, status "
                + "FROM orders WHERE restaurant_id = ? ORDER BY id DESC";
        String itemSql = "SELECT food_name, quantity, unit_price FROM order_items WHERE order_id = ?";

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, restaurantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Order order = new Order(
                                rs.getInt("id"),
                                rs.getString("customer_name"),
                                rs.getInt("restaurant_id"),
                                rs.getString("restaurant_name"));
                        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                        orders.add(order);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (Order order : orders) {
                    ps.setInt(1, order.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            order.addItem(new OrderItem(
                                    rs.getString("food_name"),
                                    rs.getInt("quantity"),
                                    rs.getDouble("unit_price")));
                        }
                    }
                }
            }
        }

        return orders;
    }

    /** Loads every order placed by one customer, most recent first, each with its items. Used by order history. */
    public List<Order> findByCustomer(String customerName) throws SQLException {
        String orderSql = "SELECT id, customer_name, restaurant_id, restaurant_name, status, created_at "
                + "FROM orders WHERE customer_name = ? ORDER BY id DESC";
        String itemSql = "SELECT food_name, quantity, unit_price FROM order_items WHERE order_id = ?";

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setString(1, customerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Order order = new Order(
                                rs.getInt("id"),
                                rs.getString("customer_name"),
                                rs.getInt("restaurant_id"),
                                rs.getString("restaurant_name"));
                        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                        order.setCreatedAt(formatTimestamp(rs.getString("created_at")));
                        orders.add(order);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (Order order : orders) {
                    ps.setInt(1, order.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            order.addItem(new OrderItem(
                                    rs.getString("food_name"),
                                    rs.getInt("quantity"),
                                    rs.getDouble("unit_price")));
                        }
                    }
                }
            }
        }

        return orders;
    }

    /** Converts the stored LocalDateTime.toString() text into a friendlier display format. */
    private String formatTimestamp(String raw) {
        try {
            return LocalDateTime.parse(raw).format(DISPLAY_FORMAT);
        } catch (Exception e) {
            return raw; // fall back to the raw text rather than crashing the history screen
        }
    }
}