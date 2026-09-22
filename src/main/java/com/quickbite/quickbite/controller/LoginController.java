package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    // Phase-1 placeholder credentials. NOT real security: a later phase replaces this.
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    /** Phase 1: any non-empty username and password is accepted for customers. */
    @FXML
    private void onCustomerLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both a username and a password.");
            return;
        }
        Navigator.showRestaurants(username);
    }

    @FXML
    private void onAdminLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            Navigator.showAdmin();
        } else {
            messageLabel.setText("Invalid admin username or password.");
            passwordField.clear();
        }
    }
}