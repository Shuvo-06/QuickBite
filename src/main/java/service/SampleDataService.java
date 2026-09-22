package com.quickbite.quickbite.service;

import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies mock restaurants and menus.
 * In later phases this class will load real data from SQLite / the external API instead.
 */
public class SampleDataService {

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<>();

        Restaurant pizza = new Restaurant(1, "Pizza Palace", "Wood-fired pizzas and Italian favourites.", 4.6);
        pizza.addFood(new FoodItem(101, "Margherita Pizza", "Tomato sauce, mozzarella and fresh basil.", 250));
        pizza.addFood(new FoodItem(102, "Chicken Pizza", "Grilled chicken, capsicum and cheese.", 350));
        pizza.addFood(new FoodItem(103, "Garlic Bread", "Toasted bread with garlic butter.", 120));
        pizza.addFood(new FoodItem(104, "Cold Drink", "Chilled soft drink, 500 ml.", 60));
        restaurants.add(pizza);

        Restaurant burger = new Restaurant(2, "Burger House", "Juicy burgers and crispy fries.", 4.3);
        burger.addFood(new FoodItem(201, "Chicken Burger", "Crispy chicken fillet with mayo.", 180));
        burger.addFood(new FoodItem(202, "Beef Burger", "Beef patty, cheddar and lettuce.", 250));
        burger.addFood(new FoodItem(203, "French Fries", "Golden and lightly salted.", 100));
        burger.addFood(new FoodItem(204, "Chocolate Shake", "Thick and creamy.", 150));
        restaurants.add(burger);

        Restaurant bengal = new Restaurant(3, "Bengal Bites", "Traditional Bengali home-style meals.", 4.8);
        bengal.addFood(new FoodItem(301, "Chicken Biryani", "Fragrant rice with spiced chicken.", 280));
        bengal.addFood(new FoodItem(302, "Beef Tehari", "Rice cooked with tender beef.", 240));
        bengal.addFood(new FoodItem(303, "Beef Bhuna", "Slow-cooked beef in rich masala.", 320));
        bengal.addFood(new FoodItem(304, "Mishti Doi", "Sweet traditional yogurt.", 70));
        restaurants.add(bengal);

        Restaurant noodle = new Restaurant(4, "Noodle Station", "Fresh noodles and Asian street food.", 4.4);
        noodle.addFood(new FoodItem(401, "Chicken Chow Mein", "Stir-fried noodles with chicken.", 220));
        noodle.addFood(new FoodItem(402, "Thai Soup", "Hot and sour soup with vegetables.", 200));
        noodle.addFood(new FoodItem(403, "Vegetable Fried Rice", "Wok-fried rice with fresh vegetables.", 180));
        noodle.addFood(new FoodItem(404, "Spring Rolls", "Crispy rolls with dipping sauce.", 140));
        restaurants.add(noodle);

        return restaurants;
    }
}