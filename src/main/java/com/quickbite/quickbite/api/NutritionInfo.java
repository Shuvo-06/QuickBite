package com.quickbite.quickbite.api;

import java.util.List;

/**
 * The handful of nutrition facts QuickBite shows, per 100 g. A value of -1 means USDA did not
 * report that nutrient for the matched food.
 */
public class NutritionInfo {

    private final String matchedName;
    private final double calories;
    private final double protein;
    private final double fat;
    private final double carbs;
    private final double sugar;
    private final double fiber;
    private final double sodiumMg;

    private NutritionInfo(String matchedName, double calories, double protein, double fat,
                          double carbs, double sugar, double fiber, double sodiumMg) {
        this.matchedName = matchedName;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.sugar = sugar;
        this.fiber = fiber;
        this.sodiumMg = sodiumMg;
    }

    /** Picks the nutrients we care about out of the full parsed USDA record. */
    public static NutritionInfo fromFood(FdcFood food) {
        List<FdcNutrient> nutrients = food.getFoodNutrients() == null ? List.of() : food.getFoodNutrients();

        double calories = amountOf(nutrients, "208");
        if (calories < 0) {
            calories = firstWithUnit(nutrients, "KCAL"); // some foods report energy under another number
        }
        return new NutritionInfo(food.getDescription(), calories,
                amountOf(nutrients, "203"), amountOf(nutrients, "204"), amountOf(nutrients, "205"),
                amountOf(nutrients, "269"), amountOf(nutrients, "291"), amountOf(nutrients, "307"));
    }

    private static double amountOf(List<FdcNutrient> nutrients, String number) {
        for (FdcNutrient nutrient : nutrients) {
            if (number.equals(nutrient.getNumber())) {
                return nutrient.getValue();
            }
        }
        return -1;
    }

    private static double firstWithUnit(List<FdcNutrient> nutrients, String unit) {
        for (FdcNutrient nutrient : nutrients) {
            if (unit.equalsIgnoreCase(nutrient.getUnit())) {
                return nutrient.getValue();
            }
        }
        return -1;
    }

    /** "12.3 g", or "not available" for a missing value. */
    public static String describe(double value, String unit) {
        return value < 0 ? "not available" : String.format("%.1f %s", value, unit);
    }

    public String getMatchedName() { return matchedName; }
    public double getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getFat() { return fat; }
    public double getCarbs() { return carbs; }
    public double getSugar() { return sugar; }
    public double getFiber() { return fiber; }
    public double getSodiumMg() { return sodiumMg; }
}