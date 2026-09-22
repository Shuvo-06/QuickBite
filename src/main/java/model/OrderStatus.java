package com.quickbite.quickbite.model;

/**
 * Every step of an order's journey.
 * The ORDER of the constants below is the delivery order (next() relies on it).
 */
public enum OrderStatus {
    PLACED("Order Placed"),
    CONFIRMED("Order Confirmed"),
    PREPARING("Restaurant Preparing"),
    READY("Ready for Delivery"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Text shown to the customer. */
    public String getLabel() {
        return label;
    }

    /** Returns the next status, or null if the order is already finished. */
    public OrderStatus next() {
        return isFinished() ? null : values()[ordinal() + 1];
    }

    public boolean isFinished() {
        return this == DELIVERED;
    }
}