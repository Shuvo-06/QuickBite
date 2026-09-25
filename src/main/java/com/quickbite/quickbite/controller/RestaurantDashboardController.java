package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.OrderDAO;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.OrderStatus;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.service.OrderService;
import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class RestaurantDashboardController {

    @FXML private Label restaurantNameLabel;
    @FXML private Label messageLabel;
    @FXML private VBox incomingOrdersBox;
    @FXML private VBox activeOrdersBox;
    @FXML private VBox completedOrdersBox;

    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderService orderService = new OrderService();

    private Restaurant restaurant;

    // Stored in a field so the SAME object can be removed again in dispose().
    private final Consumer<Order> orderListener = this::onOrderEvent;

    /** Called by Navigator right after loading, to pass which restaurant is logged in. */
    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
        restaurantNameLabel.setText(restaurant.getName());
        loadOrders();
        OrderTrackingService.getInstance().addListener(orderListener);
    }

    /** Called by Navigator when this window is hidden or replaced. */
    public void dispose() {
        OrderTrackingService.getInstance().removeListener(orderListener);
    }

    /** Called on the JavaFX thread whenever ANY order changes, for any restaurant. */
    private void onOrderEvent(Order order) {
        if (order.getRestaurantId() == restaurant.getId()) {
            loadOrders(); // simplest correct option: reload this restaurant's orders from SQLite
        }
    }

    private void loadOrders() {
        try {
            List<Order> orders = orderDAO.findByRestaurant(restaurant.getId());
            renderOrders(orders);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not load orders from the database.");
        }
    }

    private void renderOrders(List<Order> orders) {
        incomingOrdersBox.getChildren().clear();
        activeOrdersBox.getChildren().clear();
        completedOrdersBox.getChildren().clear();

        boolean hasIncoming = false, hasActive = false, hasCompleted = false;

        for (Order order : orders) {
            OrderStatus status = order.getStatus();
            if (status == OrderStatus.PLACED) {
                incomingOrdersBox.getChildren().add(createIncomingCard(order));
                hasIncoming = true;
            } else if (status.isFinished()) {
                completedOrdersBox.getChildren().add(createReadOnlyCard(order));
                hasCompleted = true;
            } else {
                activeOrdersBox.getChildren().add(createReadOnlyCard(order));
                hasActive = true;
            }
        }

        if (!hasIncoming) incomingOrdersBox.getChildren().add(emptyLabel("No new orders right now."));
        if (!hasActive) activeOrdersBox.getChildren().add(emptyLabel("No orders currently in progress."));
        if (!hasCompleted) completedOrdersBox.getChildren().add(emptyLabel("No completed orders yet."));
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-text");
        return label;
    }

    private VBox createIncomingCard(Order order) {
        VBox card = baseCard(order);

        Button accept = new Button("Accept", new FontIcon("fas-check"));
        accept.getStyleClass().add("primary-button");
        accept.setOnAction(e -> handleAccept(order));

        Button reject = new Button("Reject", new FontIcon("fas-times"));
        reject.getStyleClass().add("secondary-button");
        reject.setOnAction(e -> handleReject(order));

        card.getChildren().add(new HBox(10, accept, reject));
        return card;
    }

    private VBox createReadOnlyCard(Order order) {
        VBox card = baseCard(order);
        Label status = new Label(order.getStatus().getLabel());
        status.getStyleClass().add(order.getStatus() == OrderStatus.REJECTED ? "error-label" : "info-label");
        card.getChildren().add(status);
        return card;
    }

    private VBox baseCard(Order order) {
        Label title = new Label("Order #" + order.getId() + " - " + order.getCustomerName());
        title.getStyleClass().add("card-title");

        VBox itemsBox = new VBox(2);
        for (OrderItem item : order.getItems()) {
            Label line = new Label(item.getFoodName() + " x " + item.getQuantity());
            line.getStyleClass().add("card-text");
            itemsBox.getChildren().add(line);
        }

        Label total = new Label("Total: " + PriceFormatter.format(order.getTotal()));
        total.getStyleClass().add("price-label");

        VBox card = new VBox(8, title, itemsBox, total);
        card.getStyleClass().add("food-card");
        return card;
    }

    private void handleAccept(Order order) {
        try {
            orderService.acceptOrder(order);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not accept the order. Please try again.");
            return;
        }
        OrderTrackingService.getInstance().notifyListeners(order); // tell the customer right away
        OrderTrackingService.getInstance().track(order);           // start automatic progression
        loadOrders();
    }

    private void handleReject(Order order) {
        try {
            orderService.rejectOrder(order);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not reject the order. Please try again.");
            return;
        }
        OrderTrackingService.getInstance().notifyListeners(order);
        loadOrders();
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}