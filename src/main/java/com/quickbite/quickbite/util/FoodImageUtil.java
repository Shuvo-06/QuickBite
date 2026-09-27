package com.quickbite.quickbite.util;

import javafx.scene.image.Image;

import java.util.Locale;

/**
 * Decides which picture to show for a food item or restaurant.
 * If the admin has set an image URL (including a Google image link), that is used directly —
 * JavaFX's Image class loads remote URLs on its own background thread. Otherwise, a bundled
 * local placeholder is chosen by matching keywords in the name, the same approach FoodIconUtil
 * uses for icons, so every dish always has SOME picture even with no internet connection.
 */
public class FoodImageUtil {

    private static final String IMAGES_FOLDER = "/com/quickbite/quickbite/images/";

    private FoodImageUtil() {
    }

    public static Image imageForFood(String foodName, String adminImageUrl) {
        if (adminImageUrl != null && !adminImageUrl.isBlank()) {
            return loadRemote(adminImageUrl);
        }
        return loadLocal(localFileFor(foodName));
    }

    public static Image imageForRestaurant(String adminImageUrl) {
        if (adminImageUrl != null && !adminImageUrl.isBlank()) {
            return loadRemote(adminImageUrl);
        }
        return loadLocal("restaurant.png");
    }

    private static String localFileFor(String foodName) {
        String name = foodName.toLowerCase(Locale.ROOT);

        if (name.contains("pizza")) return "pizza.png";
        if (name.contains("burger")) return "burger.png";
        if (name.contains("bread") || name.contains("naan")) return "bread.png";
        if (name.contains("drink") || name.contains("shake") || name.contains("juice")) return "drink.png";
        if (name.contains("curry") || name.contains("bhuna") || name.contains("chicken")
                || name.contains("biryani") || name.contains("tehari")) return "curry.png";
        if (name.contains("roll")) return "roll.png";
        if (name.contains("doi") || name.contains("sweet") || name.contains("dessert")) return "dessert.png";
        if (name.contains("soup")) return "soup.png";

        return "generic.png";
    }

    private static Image loadLocal(String fileName) {
        // "true" enables background loading, matching how remote URLs are loaded, so a slow disk
        // read never blocks the JavaFX Application Thread either.
        return new Image(FoodImageUtil.class.getResource(IMAGES_FOLDER + fileName).toExternalForm(),
                0, 0, true, true, true);
    }

    private static Image loadRemote(String url) {
        return new Image(url, 0, 0, true, true, true);
    }
}
