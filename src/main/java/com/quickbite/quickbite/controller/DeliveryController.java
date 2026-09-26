package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class DeliveryController {

    @FXML private Label orderTitleLabel;
    @FXML private Label customerLabel;
    @FXML private Label restaurantLabel;
    @FXML private VBox itemsBox;
    @FXML private Label totalLabel;
    @FXML private Label statusMessageLabel;
    @FXML private VBox stepsBox;

    private Order order;

    @FXML
    private void onOrderHistory() {
        Navigator.showOrderHistory(order.getCustomerName());
    }

    // Stored in a field so the SAME object can be removed again in dispose().
    private final Consumer<Order> statusListener = this::onOrderUpdated;

    /** Called by Navigator right after loading, to pass the order to display. */
    public void setOrder(Order order) {
        this.order = order;

        orderTitleLabel.setText("Order #" + order.getId());
        customerLabel.setText(order.getCustomerName());
        restaurantLabel.setText(order.getRestaurantName());
        totalLabel.setText(PriceFormatter.format(order.getTotal()));

        itemsBox.getChildren().clear();
        for (OrderItem item : order.getItems()) {
            Label name = new Label(item.getFoodName() + " x " + item.getQuantity());
            name.getStyleClass().add("item-text");

            Label price = new Label(PriceFormatter.format(item.getSubtotal()));
            price.getStyleClass().add("item-text");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            itemsBox.getChildren().add(new HBox(name, spacer, price));
        }

        refreshStatus();

        // From now on the tracker tells us whenever an order changes.
        OrderTrackingService.getInstance().addListener(statusListener);
    }

    /** Called by Navigator when this window is hidden or replaced. */
    public void dispose() {
        OrderTrackingService.getInstance().removeListener(statusListener);
    }

    /** Called on the JavaFX thread by OrderTrackingService after ANY order changed. */
    private void onOrderUpdated(Order updatedOrder) {
        if (updatedOrder.getId() == order.getId()) {
            refreshStatus();
        }
    }

    /** Redraws the tracker from order.getStatus(). Only ever called on the JavaFX thread. */
    private void refreshStatus() {
        OrderStatus current = order.getStatus();
        stepsBox.getChildren().clear();

        if (current == OrderStatus.REJECTED) {
            Label rejected = new Label("This order was rejected by the restaurant.");
            rejected.getStyleClass().add("error-label");
            rejected.setWrapText(true);
            stepsBox.getChildren().add(rejected);
            statusMessageLabel.setText("Sorry, " + order.getRestaurantName() + " was unable to accept this order.");
            return;
        }

        for (OrderStatus step : OrderStatus.mainSequence()) {
            String state;
            String iconCode;

            if (step.ordinal() < current.ordinal() || (step == current && current.isFinished())) {
                state = "done";
                iconCode = "fas-check-circle";
            } else if (step == current) {
                state = "current";
                iconCode = "fas-dot-circle";
            } else {
                state = "pending";
                iconCode = "far-circle";
            }

            FontIcon icon = new FontIcon(iconCode);
            icon.getStyleClass().add("step-icon-" + state);

            Label text = new Label(step.getLabel());
            text.getStyleClass().add("step-text-" + state);

            HBox row = new HBox(12, icon, text);
            row.setAlignment(Pos.CENTER_LEFT);
            stepsBox.getChildren().add(row);
        }

        if (current.isFinished()) {
            statusMessageLabel.setText("Your order has been delivered. Enjoy your meal!");
        } else if (current == OrderStatus.PLACED) {
            statusMessageLabel.setText("Waiting for the restaurant to confirm your order...");
        } else {
            statusMessageLabel.setText("Current status: " + current.getLabel());
        }
    }

    @FXML
    private void onBackToRestaurants() {
        Navigator.showRestaurants(order.getCustomerName());
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}