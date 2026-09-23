package com.quickbite.quickbite;

import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.NotificationHelper;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The JavaFX Application class.
 * JavaFX calls start() on the JavaFX Application Thread once the toolkit is ready.
 */
public class QuickBiteApp extends Application {

    @Override
    public void start(Stage primaryStage) {
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