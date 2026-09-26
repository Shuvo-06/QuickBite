package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Placeholder admin dashboard. Each section can be given a real screen in a later phase. */
public class AdminController {

    @FXML
    private Label infoLabel;

    @FXML
    private void onRestaurants() {
        showComingSoon("Restaurant management");
    }

    @FXML
    private void onFoodItems() {
        showComingSoon("Food item management");
    }

    @FXML
    private void onOrders() {
        showComingSoon("Order management");
    }

    @FXML
    private void onDeliveryManagement() {
        showComingSoon("Delivery management");
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }

    private void showComingSoon(String section) {
        infoLabel.setText(section + " will be available in a later phase.");
    }
}
