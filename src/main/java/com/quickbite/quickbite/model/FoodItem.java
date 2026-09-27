package com.quickbite.quickbite.model;

public class FoodItem {
    private final int id;
    private final String name;
    private final String description;
    private final double price;
    private final String imageUrl; // admin-provided picture (e.g. a Google image link), or null

    public FoodItem(int id, String name, String description, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}
