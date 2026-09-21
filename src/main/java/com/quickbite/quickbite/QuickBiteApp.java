package com.quickbite.quickbite;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * The JavaFX Application class.
 * JavaFX calls start() on the JavaFX Application Thread once the toolkit is ready.
 */
public class QuickBiteApp extends Application {

    private static final String WELCOME_FXML = "/com/quickbite/quickbite/fxml/welcome.fxml";
    private static final String MAIN_CSS = "/com/quickbite/quickbite/css/quickbite.css";

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Load the FXML (the screen layout)
        URL fxmlUrl = QuickBiteApp.class.getResource(WELCOME_FXML);
        if (fxmlUrl == null) {
            throw new IllegalStateException("Cannot find FXML file: " + WELCOME_FXML);
        }
        Parent root = new FXMLLoader(fxmlUrl).load();

        // 2. Put it in a Scene
        Scene scene = new Scene(root, 1000, 650);

        // 3. Attach the CSS to the Scene (applies to every node inside it)
        URL cssUrl = QuickBiteApp.class.getResource(MAIN_CSS);
        if (cssUrl == null) {
            throw new IllegalStateException("Cannot find CSS file: " + MAIN_CSS);
        }
        scene.getStylesheets().add(cssUrl.toExternalForm());

        // 4. Show the window (the Stage)
        stage.setTitle("QuickBite");
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * JavaFX calls stop() when the window is closed.
     * In Phase 7 we will shut down our ExecutorService here.
     */
    @Override
    public void stop() {
        System.out.println("QuickBite is closing.");
    }
}