package com.quickbite.quickbite;

import com.quickbite.quickbite.database.DatabaseInitializer;
import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.NotificationHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;

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

        // The login screen uses the primary stage that JavaFX gives us.
        Navigator.showLogin(primaryStage);
    }

    /**
     * JavaFX calls stop() when the last window is closed.
     * We must stop the background threads here.
     */
    @Override
    public void stop() {
        OrderTrackingService.getInstance().shutdown();
        System.out.println("QuickBite is closing.");
    }
}