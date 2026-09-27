package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.UserDAO;
import com.quickbite.quickbite.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please choose a username and a password.");
            return;
        }
        if (!password.equals(confirm)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            if (userDAO.exists(username)) {
                messageLabel.setText("That username is already taken.");
                return;
            }
            userDAO.register(username, password);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not create your account. Please try again.");
            return;
        }

        // Registering logs the customer straight in, the same way placing an order used to.
        Navigator.showRestaurants(username);
    }

    @FXML
    private void onBackToLogin() {
        Navigator.showLogin();
    }
}
