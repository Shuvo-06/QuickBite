package com.quickbite.quickbite.util;

import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderStatus;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

/** Shows ControlsFX pop-up notifications for order updates. Must be called on the JavaFX thread. */
public class NotificationHelper {

    private NotificationHelper() {
    }

    public static void showOrderUpdate(Order order) {
        // Only tell the customer who owns this order
        if (!order.getCustomerName().equals(Navigator.getCurrentUsername())) {
            return;
        }

        // The pop-up needs a visible window to attach to
        Stage owner = Navigator.getCurrentStage();
        if (owner == null || !owner.isShowing()) {
            return;
        }

        OrderStatus status = order.getStatus();

        Notifications notification = Notifications.create()
                .title("Order #" + order.getId() + " - " + order.getRestaurantName())
                .text(status.getLabel() + "\nClick to track this order")
                .owner(owner)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(6))
                // runLater: let the click finish before we close the window the pop-up belongs to
                .onAction(event -> Platform.runLater(() -> Navigator.showDelivery(order)));

        if (status.isFinished()) {
            notification.showConfirm();       // green tick for "Delivered"
        } else {
            notification.showInformation();
        }
    }
}
