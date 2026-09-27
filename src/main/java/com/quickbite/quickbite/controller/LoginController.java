package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.UserDAO;
import com.quickbite.quickbite.model.User;
import com.quickbite.quickbite.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();

    /** Customer accounts are now real, persistent rows in the users table (see UserDAO). */
    @FXML
    private void onCustomerLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both a username and a password.");
            return;
        }

        User account;
        try {
            account = userDAO.findByUsername(username);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not reach the database. Please try again.");
            return;
        }

        if (account == null) {
            messageLabel.setText("No account with that username. Use Create Account below.");
            return;
        }
        if (!account.getPassword().equals(password)) {
            messageLabel.setText("Incorrect password.");
            passwordField.clear();
            return;
        }

        Navigator.showRestaurants(account.getUsername());
    }

    @FXML
    private void onGoToRegister() {
        Navigator.showRegister();
    }

    @FXML
    private void onRestaurantLogin() {
        Navigator.showRestaurantLogin();
    }

    /**
     * The admin login intentionally performs NO password check: any non-empty username and
     * password logs straight into the admin dashboard. This is a deliberate simplification asked
     * for explicitly, matching the same "Phase 1 placeholder" spirit as the rest of the login
     * screens — it is not meant to represent real security.
     */
    @FXML
    private void onAdminLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both a username and a password for admin access.");
            return;
        }
        Navigator.showAdmin();
    }
}
