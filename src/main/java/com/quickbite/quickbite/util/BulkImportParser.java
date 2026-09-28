package com.quickbite.quickbite.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns pasted text into restaurants and foods for the admin's Bulk Add. One item per line,
 * fields separated by "|":
 *
 *   RESTAURANT: Name | Description | Rating | Password | Image (optional)
 *   Food Name | Description | Price | Image (optional)     <- belongs to the RESTAURANT line above
 *
 * Blank lines and lines starting with # are ignored. Food lines that appear before any
 * RESTAURANT line are returned separately as "orphans", for the admin's currently selected restaurant.
 */
public class BulkImportParser {

    public record ParsedFood(String name, String description, double price, String image) { }

    public static class ParsedRestaurant {
        public final String name;
        public final String description;
        public final double rating;
        public final String password;
        public final String image;
        public final List<ParsedFood> foods = new ArrayList<>();

        ParsedRestaurant(String name, String description, double rating, String password, String image) {
            this.name = name;
            this.description = description;
            this.rating = rating;
            this.password = password;
            this.image = image;
        }
    }

    public record Result(List<ParsedRestaurant> restaurants, List<ParsedFood> orphanFoods, List<String> errors) { }

    private static final String RESTAURANT_PREFIX = "RESTAURANT:";

    private BulkImportParser() {
    }

    public static Result parse(String text) {
        List<ParsedRestaurant> restaurants = new ArrayList<>();
        List<ParsedFood> orphanFoods = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        ParsedRestaurant current = null;

        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNumber = i + 1;
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.regionMatches(true, 0, RESTAURANT_PREFIX, 0, RESTAURANT_PREFIX.length())) {
                String[] f = split(line.substring(RESTAURANT_PREFIX.length()));
                if (f.length < 4) {
                    errors.add("Line " + lineNumber + ": a RESTAURANT line needs Name | Description | Rating | Password");
                    continue;
                }
                Double rating = parseNumber(f[2]);
                if (f[0].isEmpty() || rating == null || rating < 0 || rating > 5) {
                    errors.add("Line " + lineNumber + ": needs a name and a rating between 0 and 5");
                    continue;
                }
                if (f[3].length() < 4) {
                    errors.add("Line " + lineNumber + ": the password must be at least 4 characters");
                    continue;
                }
                current = new ParsedRestaurant(f[0], f[1], rating, f[3], f.length > 4 ? f[4] : "");
                restaurants.add(current);
            } else {
                String[] f = split(line);
                if (f.length < 3) {
                    errors.add("Line " + lineNumber + ": a food line needs Name | Description | Price");
                    continue;
                }
                Double price = parseNumber(f[2]);
                if (f[0].isEmpty() || price == null || price < 0) {
                    errors.add("Line " + lineNumber + ": needs a name and a price of 0 or more");
                    continue;
                }
                ParsedFood food = new ParsedFood(f[0], f[1], price, f.length > 3 ? f[3] : "");
                if (current != null) {
                    current.foods.add(food);
                } else {
                    orphanFoods.add(food);
                }
            }
        }
        return new Result(restaurants, orphanFoods, errors);
    }

    private static String[] split(String line) {
        String[] parts = line.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private static Double parseNumber(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}