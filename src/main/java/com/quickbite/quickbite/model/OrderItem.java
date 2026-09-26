package com.quickbite.quickbite.model;

/** One line of an order: a food name, how many, and the unit price at order time. */
public class OrderItem {
    private final String foodName;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(String foodName, int quantity, double unitPrice) {
        this.foodName = foodName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getFoodName() { return foodName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    public double getSubtotal() {
        return unitPrice * quantity;
    }
}