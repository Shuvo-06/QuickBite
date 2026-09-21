package com.quickbite.quickbite.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Temporary controller for the Phase 1 test screen.
 * In Phase 3 the welcome screen will be replaced by the real Login screen.
 */
public class WelcomeController {

    // Injected by FXMLLoader: the name must match fx:id in the FXML file
    @FXML
    private Label statusLabel;

    private int clickCount = 0;

    /** Called automatically by FXMLLoader after the FXML has been loaded. */
    @FXML
    private void initialize() {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        statusLabel.setText("Running on Java " + javaVersion + " with JavaFX " + javafxVersion);
    }

    /** Called when the button is clicked (onAction="#onTestButtonClick" in the FXML). */
    @FXML
    private void onTestButtonClick() {
        clickCount++;
        statusLabel.setText("Controller works! Button clicked " + clickCount + " time(s).");
    }
}