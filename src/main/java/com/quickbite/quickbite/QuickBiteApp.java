package com.quickbite.quickbite;

import com.quickbite.quickbite.util.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The JavaFX Application class.
 * JavaFX calls start() on the JavaFX Application Thread once the toolkit is ready.
 */
public class QuickBiteApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // The login screen uses the primary stage that JavaFX gives us.
        Navigator.showLogin(primaryStage);
    }

    /**
     * JavaFX calls stop() when the last window is closed.
     * In Phase 7 we will shut down our ExecutorService here.
     */
    @Override
    public void stop() {
        System.out.println("QuickBite is closing.");
    }
}