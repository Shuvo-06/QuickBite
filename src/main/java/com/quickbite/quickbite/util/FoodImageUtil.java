package com.quickbite.quickbite.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.Locale;

/**
 * Decides which picture to show for a food item or restaurant. The admin's "Image" field in
 * each Add/Edit form accepts EITHER of two things:
 *
 *   1. A full web address ("https://...") — loaded directly. JavaFX's Image class fetches
 *      remote URLs on its own background thread, so this never blocks the JavaFX thread.
 *   2. A plain filename ("my_special_pizza.jpg") — the admin has copied that file themselves
 *      into src/main/resources/com/quickbite/quickbite/images/, and this class loads it from
 *      there instead of trying to treat it as a web address.
 *
 * If neither is set (or a named local file can't actually be found), a bundled placeholder is
 * chosen by matching keywords in the name, so every dish always has SOME picture even with no
 * internet connection and no custom image supplied.
 */
public class FoodImageUtil {

    private static final String IMAGES_FOLDER = "/com/quickbite/quickbite/images/";

    private FoodImageUtil() {
    }

    public static Image imageForFood(String foodName, String adminImage) {
        Image custom = resolveAdminImage(adminImage);
        return custom != null ? custom : loadLocal(localFileFor(foodName));
    }

    public static Image imageForRestaurant(String adminImage) {
        Image custom = resolveAdminImage(adminImage);
        return custom != null ? custom : loadLocal("restaurant.png");
    }

    /** Returns null if no admin image was set, so the caller falls back to the keyword default. */
    private static Image resolveAdminImage(String adminImage) {
        if (adminImage == null || adminImage.isBlank()) {
            return null;
        }
        String value = adminImage.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return loadRemote(value);
        }

        // Not a URL, so treat it as a filename inside the images resource folder.
        URL localFile = FoodImageUtil.class.getResource(IMAGES_FOLDER + value);
        if (localFile == null) {
            // The admin typed a filename that isn't actually there (typo, or not added yet) —
            // fall back to the keyword default rather than showing a broken image.
            return null;
        }
        return new Image(localFile.toExternalForm(), 0, 0, true, true, true);
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
        return new Image(FoodImageUtil.class.getResource(IMAGES_FOLDER + fileName).toExternalForm(),
                0, 0, true, true, true);
    }

    private static Image loadRemote(String url) {
        return new Image(url, 0, 0, true, true, true);
    }
}