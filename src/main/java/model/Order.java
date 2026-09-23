package com.quickbite.quickbite.model;

import java.util.ArrayList;
import java.util.List;

/** An order kept in memory. Later phases will save it in SQLite. */
public class Order {
    private final int id;
    private final String customerName;
    private final String restaurantName;
    private final List<OrderItem> items = new ArrayList<>();

    // volatile: written by a background thread, read by the JavaFX thread
    private volatile OrderStatus status = OrderStatus.PLACED;

    public Order(int id, String customerName, String restaurantName) {
        this.id = id;
        this.customerName = customerName;
        this.restaurantName = restaurantName;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    /** The total is always calculated from the items, so it can never get out of sync. */
    public double getTotal() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getRestaurantName() { return restaurantName; }
    public List<OrderItem> getItems() { return items; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}