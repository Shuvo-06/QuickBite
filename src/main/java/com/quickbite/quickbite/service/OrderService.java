package com.quickbite.quickbite.service;

import com.quickbite.quickbite.dao.OrderDAO;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.model.Restaurant;

import java.sql.SQLException;
import java.util.List;

/** Business logic for orders (kept out of the controllers). */
public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();

    /** Creates a new order and saves it (with its items) to the database. Status starts at PLACED. */
    public Order createOrder(String customerName, Restaurant restaurant, List<OrderItem> items) throws SQLException {
        return orderDAO.insertOrder(customerName, restaurant, items);
    }

    /**
     * Moves the order to its next status in the normal flow, in memory AND in the database.
     * Called by OrderTrackingService on a background thread once an order is CONFIRMED or later.
     */
    public void advanceStatus(Order order) throws SQLException {
        OrderStatus next = order.getStatus().next();
        if (next != null) {
            order.setStatus(next);
            orderDAO.updateStatus(order.getId(), next);
        }
    }

    /** Called by the restaurant dashboard when the restaurant accepts a PLACED order. */
    public void acceptOrder(Order order) throws SQLException {
        order.setStatus(OrderStatus.CONFIRMED);
        orderDAO.updateStatus(order.getId(), OrderStatus.CONFIRMED);
    }

    /** Called by the restaurant dashboard when the restaurant rejects a PLACED order. */
    public void rejectOrder(Order order) throws SQLException {
        order.setStatus(OrderStatus.REJECTED);
        orderDAO.updateStatus(order.getId(), OrderStatus.REJECTED);
    }
}