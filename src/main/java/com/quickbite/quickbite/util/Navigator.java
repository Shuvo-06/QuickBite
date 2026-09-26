package com.quickbite.quickbite.util;

import com.quickbite.quickbite.controller.DeliveryController;
import com.quickbite.quickbite.controller.OrderHistoryController;
import com.quickbite.quickbite.controller.RestaurantController;
import com.quickbite.quickbite.controller.RestaurantDashboardController;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.Restaurant;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    /** The customer currently logged in (null on the login, admin and restaurant screens). */
    private static String currentUsername;

    private Navigator() {
    }

    public static Stage getCurrentStage() {
        return currentStage;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    // ------------------------------------------------------------------
    // Public navigation methods (one per screen)
    // ------------------------------------------------------------------

    /** Login screen shown inside a given stage (used once at startup). */
    public static void showLogin(Stage stage) {
        try {
            FXMLLoader loader = loadFxml("login.fxml");
            currentUsername = null;
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
            currentUsername = username;
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

            Stage stage = new Stage();
            // When this window disappears (closed by the user OR replaced by the Navigator),
            // the controller stops listening for order updates.
            stage.setOnHidden(event -> controller.dispose());

            currentUsername = order.getCustomerName();
            showWindow(stage, loader, "QuickBite - Delivery Status", 950, 650);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showOrderHistory(String username) {
        try {
            FXMLLoader loader = loadFxml("order_history.fxml");
            OrderHistoryController controller = loader.getController();
            controller.setUsername(username);
            currentUsername = username;
            showWindow(new Stage(), loader, "QuickBite - Order History", 1050, 680);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showAdmin() {
        try {
            FXMLLoader loader = loadFxml("admin.fxml");
            currentUsername = null;
            showWindow(new Stage(), loader, "QuickBite - Admin", 950, 650);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showRestaurantLogin() {
        try {
            FXMLLoader loader = loadFxml("restaurant_login.fxml");
            currentUsername = null;
            showWindow(new Stage(), loader, "QuickBite - Restaurant Login", 900, 620);
        } catch (IOException e) {
            showLoadError(e);
        }
    }

    public static void showRestaurantDashboard(Restaurant restaurant) {
        try {
            FXMLLoader loader = loadFxml("restaurant_dashboard.fxml");
            RestaurantDashboardController controller = loader.getController();

            Stage stage = new Stage();
            stage.setOnHidden(event -> controller.dispose());

            controller.setRestaurant(restaurant);
            currentUsername = null; // restaurant staff, not a customer: no order pop-ups here
            showWindow(stage, loader, "QuickBite - " + restaurant.getName() + " Dashboard", 1150, 720);
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

    /** Puts the loaded screen into a Scene, fades it in, shows it, then closes the previous window. */
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

        // Fade the new screen in, so it appears smoothly instead of popping into view.
        root.setOpacity(0);
        stage.show();
        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        if (currentStage != null && currentStage != stage) {
            currentStage.close();                       // close the old window after the new one shows
        }
        currentStage = stage;
    }

    private static void showLoadError(IOException e) {
        e.printStackTrace(); // details for the developer console only
        Alert alert = new Alert(Alert.AlertType.ERROR, "Sorry, this screen could not be opened.");
        alert.setHeaderText("Screen error");
        alert.showAndWait();
    }
}
