package com.quickbite.quickbite.model;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private final int id;
    private final String name;
    private final String description;
    private final double rating;
    private final boolean active;      // false = blacklisted by admin, hidden from customers
    private final String imageUrl;     // admin-provided picture (e.g. a Google image link), or null
    private final List<FoodItem> menu = new ArrayList<>();

    public Restaurant(int id, String name, String description, double rating, boolean active, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rating = rating;
        this.active = active;
        this.imageUrl = imageUrl;
    }

    public void addFood(FoodItem food) {
        menu.add(food);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
    public boolean isActive() { return active; }
    public String getImageUrl() { return imageUrl; }
    public List<FoodItem> getMenu() { return menu; }

    /** Used by ComboBox (restaurant login) and any other control that needs a plain text label. */
    @Override
    public String toString() {
        return name;
    }
}
