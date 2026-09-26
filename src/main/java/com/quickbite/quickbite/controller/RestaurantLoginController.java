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

public class RestaurantLoginController {

    // Phase-1-style placeholder password, same approach as the admin login. NOT real security.
    private static final String RESTAURANT_PASSWORD = "restaurant123";

    @FXML private ComboBox<Restaurant> restaurantComboBox;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void initialize() {
        try {
            List<Restaurant> restaurants = new RestaurantDAO().findAll();
            restaurantComboBox.getItems().addAll(restaurants);
            if (!restaurants.isEmpty()) {
                restaurantComboBox.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not load restaurants from the database.");
        }
    }

    @FXML
    private void onLogin() {
        Restaurant selected = restaurantComboBox.getValue();
        if (selected == null) {
            messageLabel.setText("Please select a restaurant.");
            return;
        }
        if (!RESTAURANT_PASSWORD.equals(passwordField.getText())) {
            messageLabel.setText("Incorrect password.");
            passwordField.clear();
            return;
        }
        Navigator.showRestaurantDashboard(selected);
    }

    @FXML
    private void onBack() {
        Navigator.showLogin();
    }
}
