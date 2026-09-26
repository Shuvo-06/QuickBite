package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.OrderDAO;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class OrderHistoryController {

    @FXML private Label welcomeLabel;
    @FXML private Label messageLabel;
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> restaurantColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, String> totalColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private VBox detailBox;

    private final OrderDAO orderDAO = new OrderDAO();
    private String username;

    /** Runs automatically after the FXML is loaded. Wires up how each column reads an Order. */
    @FXML
    private void initialize() {
        // Lambda cell-value factories: no reflection needed, so no extra module-info "opens" line.
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        restaurantColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRestaurantName()));
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCreatedAt()));
        totalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(PriceFormatter.format(data.getValue().getTotal())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().getLabel()));

        orderTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldOrder, newOrder) -> showDetails(newOrder));
    }

    /** Called by Navigator right after loading, to pass the logged-in username. */
    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Order history for " + username);
        loadOrders();
    }

    private void loadOrders() {
        try {
            List<Order> orders = orderDAO.findByCustomer(username);
            ObservableList<Order> data = FXCollections.observableArrayList(orders);
            orderTable.setItems(data);

            if (orders.isEmpty()) {
                messageLabel.setText("You have not placed any orders yet.");
            } else {
                messageLabel.setText("");
                orderTable.getSelectionModel().selectFirst(); // show details for the most recent order immediately
            }
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not load your order history.");
        }
    }

    private void showDetails(Order order) {
        detailBox.getChildren().clear();

        if (order == null) {
            Label hint = new Label("Select an order above to see its details.");
            hint.getStyleClass().add("card-text");
            detailBox.getChildren().add(hint);
            return;
        }

        Label title = new Label("Order #" + order.getId());
        title.getStyleClass().add("card-title");

        Label restaurant = new Label(order.getRestaurantName());
        restaurant.getStyleClass().add("card-text");

        Label status = new Label(order.getStatus().getLabel());
        status.getStyleClass().add("info-label");

        VBox itemsBox = new VBox(6);
        for (OrderItem item : order.getItems()) {
            Label name = new Label(item.getFoodName() + " x " + item.getQuantity());
            name.getStyleClass().add("item-text");

            Label price = new Label(PriceFormatter.format(item.getSubtotal()));
            price.getStyleClass().add("item-text");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            itemsBox.getChildren().add(new HBox(name, spacer, price));
        }

        Label total = new Label("Total: " + PriceFormatter.format(order.getTotal()));
        total.getStyleClass().add("total-label");

        detailBox.getChildren().addAll(title, restaurant, status, itemsBox, total);
    }

    @FXML
    private void onBack() {
        Navigator.showRestaurants(username);
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}