package com.quickbite.quickbite.util;

import java.util.Locale;

/** Picks a food icon based on keywords in the food's name. Purely cosmetic — never affects logic. */
public class FoodIconUtil {

    private FoodIconUtil() {
    }

    public static String iconFor(String foodName) {
        String name = foodName.toLowerCase(Locale.ROOT);

        if (name.contains("pizza")) return "fas-pizza-slice";
        if (name.contains("burger")) return "fas-hamburger";
        if (name.contains("bread") || name.contains("naan")) return "fas-bread-slice";
        if (name.contains("drink") || name.contains("shake") || name.contains("juice")) return "fas-glass-whiskey";
        if (name.contains("curry") || name.contains("bhuna") || name.contains("chicken")) return "fas-drumstick-bite";
        if (name.contains("roll")) return "fas-hotdog";
        if (name.contains("doi") || name.contains("sweet") || name.contains("dessert")) return "fas-ice-cream";
        if (name.contains("soup")) return "fas-mug-hot";

        return "fas-utensils"; // safe default for rice, noodles, fries, etc.
    }
}
