package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.RestaurantDAO;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.sql.SQLException;
import java.util.List;

/**
 * One login screen for both restaurants and the administrator. Restaurants use the password the
 * admin chose when creating them (stored per restaurant in the database).
 */
public class RestaurantLoginController {

    // The administrator's password lives in code, NOT in the database, so "Reset All Data" in the
    // admin panel can never wipe it.
    private static final String ADMIN_PASSWORD = "admin123";

    // A stand-in list entry for the administrator. Id 0 can never clash with a real database id.
    private static final Restaurant ADMIN_ENTRY = new Restaurant(0, "Administrator", "", 0, true, null);

    @FXML private ComboBox<Restaurant> restaurantComboBox;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    @FXML
    private void initialize() {
        restaurantComboBox.getItems().add(ADMIN_ENTRY);
        try {
            // Every restaurant can log in here, even a blacklisted one.
            restaurantComboBox.getItems().addAll(restaurantDAO.findAllIncludingInactive());
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not load restaurants from the database.");
        }
        restaurantComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onLogin() {
        Restaurant selected = restaurantComboBox.getValue();
        if (selected == null) {
            messageLabel.setText("Please select an account.");
            return;
        }
        String password = passwordField.getText();

        if (selected == ADMIN_ENTRY) {
            if (!ADMIN_PASSWORD.equals(password)) {
                messageLabel.setText("Incorrect password.");
                passwordField.clear();
                return;
            }
            Navigator.showAdmin();
            return;
        }

        try {
            if (!restaurantDAO.checkPassword(selected.getId(), password)) {
                messageLabel.setText("Incorrect password.");
                passwordField.clear();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not check the password. Please try again.");
            return;
        }
        Navigator.showRestaurantDashboard(selected);
    }

    @FXML
    private void onBack() {
        Navigator.showLogin();
    }
}