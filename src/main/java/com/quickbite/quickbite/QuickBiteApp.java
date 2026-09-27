package com.quickbite.quickbite;

import com.quickbite.quickbite.api.ApiService;
import com.quickbite.quickbite.database.DatabaseInitializer;
import com.quickbite.quickbite.service.NetworkMonitor;
import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.service.Shutdownable;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.NotificationHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * The JavaFX Application class.
 * JavaFX calls start() on the JavaFX Application Thread once the toolkit is ready.
 */
public class QuickBiteApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 0. Create the database file and tables if they do not exist yet
        try {
            DatabaseInitializer.initialize();
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "QuickBite could not open its database. The application will close.");
            alert.setHeaderText("Database error");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // Show a pop-up whenever ANY order changes status (registered once for the whole app)
        OrderTrackingService.getInstance().addListener(NotificationHelper::showOrderUpdate);

        // NetworkMonitor's background checker starts as soon as its singleton is first touched;
        // touching it here (rather than waiting for a screen to need it) means the very first
        // check has already had a moment to run before the customer reaches the ordering screen.
        NetworkMonitor.getInstance();

        // The login screen uses the primary stage that JavaFX gives us.
        Navigator.showLogin(primaryStage);
    }

    /**
     * JavaFX calls stop() when the last window is closed.
     * Every background service implements Shutdownable, so they can all be stopped the same way
     * through one interface — stop() doesn't need to know how each one works internally.
     */
    @Override
    public void stop() {
        List<Shutdownable> backgroundServices = List.of(
                OrderTrackingService.getInstance(),
                ApiService.getInstance(),
                NetworkMonitor.getInstance()
        );
        for (Shutdownable service : backgroundServices) {
            service.shutdown();
        }
        System.out.println("QuickBite is closing.");
    }
}