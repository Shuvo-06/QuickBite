package com.quickbite.quickbite.service;

import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.model.Restaurant;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic for orders (kept out of the controllers).
 * Everything is in memory for now.
 */
public class OrderService {

    // AtomicInteger is safe to use from several threads.
    private static final AtomicInteger NEXT_ORDER_ID = new AtomicInteger(1001);

    /** Creates a new order with status PLACED. */
    public Order createOrder(String customerName, Restaurant restaurant, List<OrderItem> items) {
        Order order = new Order(NEXT_ORDER_ID.getAndIncrement(), customerName, restaurant.getName());
        for (OrderItem item : items) {
            order.addItem(item);
        }
        return order;
    }

    /**
     * Moves the order to its next status.
     * Called by OrderTrackingService on a background thread.
     */
    public void advanceStatus(Order order) {
        OrderStatus next = order.getStatus().next();
        if (next != null) {
            order.setStatus(next);
        }
    }
}