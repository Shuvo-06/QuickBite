package com.quickbite.quickbite.model;

/**
 * Every step an order can be in.
 * The normal flow is PLACED -> CONFIRMED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED.
 * REJECTED is a separate dead end set by the restaurant instead of the automatic tracker.
 */
public enum OrderStatus {
    PLACED("Order Placed"),
    CONFIRMED("Order Confirmed"),
    PREPARING("Restaurant Preparing"),
    READY("Ready for Delivery"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    REJECTED("Rejected");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Returns the next status in the normal flow, or null if the order cannot move forward automatically. */
    public OrderStatus next() {
        return isFinished() ? null : values()[ordinal() + 1];
    }

    public boolean isFinished() {
        return this == DELIVERED || this == REJECTED;
    }

    /** The statuses that make up the visual progress tracker (REJECTED is shown separately). */
    public static OrderStatus[] mainSequence() {
        return new OrderStatus[] { PLACED, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED };
    }
}