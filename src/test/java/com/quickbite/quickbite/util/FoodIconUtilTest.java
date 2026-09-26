package com.quickbite.quickbite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodIconUtilTest {

    @Test
    void picksPizzaIconForPizzaNames() {
        assertEquals("fas-pizza-slice", FoodIconUtil.iconFor("Chicken Pizza"));
    }

    @Test
    void picksBurgerIconForBurgerNames() {
        assertEquals("fas-hamburger", FoodIconUtil.iconFor("Beef Burger"));
    }

    @Test
    void fallsBackToDefaultIconForUnknownNames() {
        assertEquals("fas-utensils", FoodIconUtil.iconFor("Vegetable Fried Rice"));
    }
}
