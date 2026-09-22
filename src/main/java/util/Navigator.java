package com.quickbite.quickbite.util;

import com.quickbite.quickbite.controller.DeliveryController;
import com.quickbite.quickbite.controller.RestaurantController;
import com.quickbite.quickbite.model.Order;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Opens QuickBite's windows. Every screen is its own Stage (window);
 * when a new window appears, the previous one is closed, so only one is ever open.
 */
public class Navigator {

    private static final String FXML_FOLDER = "/com/quickbite/quickbite/fxml/";
    private static final String MAIN_CSS = "/com/quickbite/quickbite/css/quickbite.css";

    /** The window currently on screen. */
    private static Stage currentStage;

    private Navigator() {
    }

    // ------------------------------------------------------------------
    // Public navigation methods (one per screen)
    // ------------------------------------------------------------------

    /** Login screen shown inside a given stage (used once at startup). */
    public static void showLogin(Stage stage) {
        try {
            FXMLLoader loader = loadFxml("login.fxml");
            showWindow(stage, loader, "QuickBite - Login", 900, 620);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    /** Login screen in a new window (used after Logout). */
    public static void showLogin() {
        showLogin(new Stage());
    }

    public static void showRestaurants(String username) {
        try {
            FXMLLoader loader = loadFxml("restaurant.fxml");
            RestaurantController controller = loader.getController();
            controller.setUsername(username);          // pass data to the next screen
            showWindow(new Stage(), loader, "QuickBite - Restaurants", 1150, 720);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showDelivery(Order order) {
        try {
            FXMLLoader loader = loadFxml("delivery.fxml");
            DeliveryController controller = loader.getController();
            controller.setOrder(order);                // pass data to the next screen
            showWindow(new Stage(), loader, "QuickBite - Delivery Status", 950, 650);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showAdmin() {
        try {
            FXMLLoader loader = loadFxml("admin.fxml");
            showWindow(new Stage(), loader, "QuickBite - Admin", 950, 650);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Loads an FXML file. This also creates the controller and runs its initialize(). */
    private static FXMLLoader loadFxml(String fileName) throws IOException {
        URL url = Navigator.class.getResource(FXML_FOLDER + fileName);
        if (url == null) {
            throw new IOException("Cannot find FXML file: " + FXML_FOLDER + fileName);
        }
        FXMLLoader loader = new FXMLLoader(url);
        loader.load();
        return loader;
    }

    /** Puts the loaded screen into a Scene, shows it, then closes the previous window. */
    private static void showWindow(Stage stage, FXMLLoader loader, String title, double width, double height) {
        Parent root = loader.getRoot();
        Scene scene = new Scene(root, width, height);

        URL cssUrl = Navigator.class.getResource(MAIN_CSS);
        if (cssUrl == null) {
            throw new IllegalStateException("Cannot find CSS file: " + MAIN_CSS);
        }
        scene.getStylesheets().add(cssUrl.toExternalForm());

        stage.setTitle(title);
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.show();                                   // show the new window FIRST...

        if (currentStage != null && currentStage != stage) {
            currentStage.close();                       // ...then close the old one
        }
        currentStage = stage;
    }

    private static void showLoadError(IOException e) {
        e.printStackTrace();                            // details for the developer console only
        Alert alert = new Alert(Alert.AlertType.ERROR, "Sorry, this screen could not be opened.");
        alert.setHeaderText("Screen error");
        alert.showAndWait();
    }
}