package com.quickbite.quickbite.model;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private final int id;
    private final String name;
    private final String description;
    private final double rating;
    private final List<FoodItem> menu = new ArrayList<>();

    public Restaurant(int id, String name, String description, double rating) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rating = rating;
    }

    public void addFood(FoodItem food) {
        menu.add(food);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
    public List<FoodItem> getMenu() { return menu; }
}
