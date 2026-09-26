package com.quickbite.quickbite.model;

public class FoodItem {
    private final int id;
    private final String name;
    private final String description;
    private final double price;

    public FoodItem(int id, String name, String description, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
}
