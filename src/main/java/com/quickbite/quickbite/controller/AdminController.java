package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.CouponDAO;
import com.quickbite.quickbite.dao.FoodDAO;
import com.quickbite.quickbite.dao.OrderDAO;
import com.quickbite.quickbite.dao.RestaurantDAO;
import com.quickbite.quickbite.model.Coupon;
import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.service.OrderTrackingService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AdminController {

    @FXML private Label messageLabel;

    // Restaurants tab
    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, String> restNameColumn;
    @FXML private TableColumn<Restaurant, String> restRatingColumn;
    @FXML private TableColumn<Restaurant, String> restStatusColumn;

    // Coupons tab
    @FXML private TableView<Coupon> couponTable;
    @FXML private TableColumn<Coupon, String> couponCodeColumn;
    @FXML private TableColumn<Coupon, String> couponDiscountColumn;
    @FXML private TableColumn<Coupon, String> couponRangeColumn;
    @FXML private TableColumn<Coupon, String> couponStatusColumn;

    // Orders tab
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, String> orderCustomerColumn;
    @FXML private TableColumn<Order, String> orderRestaurantColumn;
    @FXML private TableColumn<Order, String> orderCouponColumn;
    @FXML private TableColumn<Order, String> orderTotalColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final FoodDAO foodDAO = new FoodDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // Reloads the "All Orders" tab the moment anything changes anywhere in the system.
    private final Consumer<Order> orderListener = order -> loadOrders();

    @FXML
    private void initialize() {
        setupRestaurantTable();
        setupCouponTable();
        setupOrderTable();

        loadRestaurants();
        loadCoupons();
        loadOrders();

        OrderTrackingService.getInstance().addListener(orderListener);
    }

    /** Called by Navigator when this window is hidden or replaced. */
    public void dispose() {
        OrderTrackingService.getInstance().removeListener(orderListener);
    }

    // ==================================================================
    // Restaurants tab
    // ==================================================================

    private void setupRestaurantTable() {
        restNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        restRatingColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getRating())));
        restStatusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isActive() ? "Whitelisted" : "Blacklisted"));
    }

    private void loadRestaurants() {
        try {
            List<Restaurant> restaurants = restaurantDAO.findAllIncludingInactive();
            restaurantTable.setItems(FXCollections.observableArrayList(restaurants));
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not load restaurants.");
        }
    }

    @FXML
    private void onAddRestaurant() {
        showRestaurantDialog(null);
    }

    @FXML
    private void onEditRestaurant() {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a restaurant first.");
            return;
        }
        showRestaurantDialog(selected);
    }

    @FXML
    private void onToggleRestaurantActive() {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a restaurant first.");
            return;
        }
        try {
            restaurantDAO.setActive(selected.getId(), !selected.isActive());
            loadRestaurants();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not update that restaurant.");
        }
    }

    @FXML
    private void onDeleteRestaurant() {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a restaurant first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + " and its whole menu? This cannot be undone.");
        confirm.setHeaderText("Delete restaurant");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            restaurantDAO.delete(selected.getId());
            loadRestaurants();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not delete that restaurant.");
        }
    }

    @FXML
    private void onManageFoods() {
        Restaurant selected = restaurantTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a restaurant first.");
            return;
        }
        showFoodManagerDialog(selected);
    }

    /** Add/edit form for one restaurant. Passing null means "create a new one". */
    private void showRestaurantDialog(Restaurant existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Restaurant" : "Edit Restaurant");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextArea descriptionField = new TextArea(existing != null ? existing.getDescription() : "");
        descriptionField.setPrefRowCount(3);
        Spinner<Double> ratingField = new Spinner<>(0.0, 5.0, existing != null ? existing.getRating() : 4.0, 0.1);
        ratingField.setEditable(true);
        TextField imageUrlField = new TextField(existing != null ? existing.getImageUrl() : "");
        imageUrlField.setPromptText("https://... (a Google image link works too) - optional");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Description"), descriptionField);
        grid.addRow(2, new Label("Rating"), ratingField);
        grid.addRow(3, new Label("Image URL"), imageUrlField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        if (nameField.getText().isBlank()) {
            messageLabel.setText("Restaurant name cannot be empty.");
            return;
        }

        try {
            if (existing == null) {
                restaurantDAO.insert(nameField.getText().trim(), descriptionField.getText().trim(),
                        ratingField.getValue(), imageUrlField.getText());
            } else {
                restaurantDAO.update(existing.getId(), nameField.getText().trim(), descriptionField.getText().trim(),
                        ratingField.getValue(), imageUrlField.getText());
            }
            messageLabel.setText("");
            loadRestaurants();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not save that restaurant.");
        }
    }

    /** A secondary window listing one restaurant's menu, with its own Add/Edit/Delete. */
    private void showFoodManagerDialog(Restaurant restaurant) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Menu for " + restaurant.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        TableView<FoodItem> table = new TableView<>();
        TableColumn<FoodItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getName()));
        nameCol.setPrefWidth(160);
        TableColumn<FoodItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(PriceFormatter.format(d.getValue().getPrice())));
        priceCol.setPrefWidth(90);
        TableColumn<FoodItem, String> imageCol = new TableColumn<>("Image");
        imageCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getImageUrl() == null ? "(default picture)" : "Custom link"));
        imageCol.setPrefWidth(120);
        table.getColumns().addAll(nameCol, priceCol, imageCol);
        table.setPrefSize(480, 300);

        Runnable refresh = () -> {
            try {
                table.setItems(FXCollections.observableArrayList(foodDAO.findByRestaurant(restaurant.getId())));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        refresh.run();

        Button addButton = new Button("Add Food");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(e -> {
            showFoodDialog(restaurant.getId(), null);
            refresh.run();
        });

        Button editButton = new Button("Edit Selected");
        editButton.getStyleClass().add("secondary-button");
        editButton.setOnAction(e -> {
            FoodItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFoodDialog(restaurant.getId(), selected);
                refresh.run();
            }
        });

        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().add("secondary-button");
        deleteButton.setOnAction(e -> {
            FoodItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    foodDAO.delete(selected.getId());
                    refresh.run();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox buttons = new HBox(10, addButton, editButton, deleteButton);
        VBox content = new VBox(10, table, buttons);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();

        loadRestaurants(); // in case the customer-facing menu size / images changed
    }

    /** Add/edit form for one food item. Passing null means "create a new one". */
    private void showFoodDialog(int restaurantId, FoodItem existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Food Item" : "Edit Food Item");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextArea descriptionField = new TextArea(existing != null ? existing.getDescription() : "");
        descriptionField.setPrefRowCount(2);
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField imageUrlField = new TextField(existing != null ? existing.getImageUrl() : "");
        imageUrlField.setPromptText("https://... (a Google image link works too) - optional");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Description"), descriptionField);
        grid.addRow(2, new Label("Price"), priceField);
        grid.addRow(3, new Label("Image URL"), imageUrlField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException e) {
            messageLabel.setText("Price must be a number.");
            return;
        }
        if (nameField.getText().isBlank() || price < 0) {
            messageLabel.setText("Please enter a valid name and a non-negative price.");
            return;
        }

        try {
            if (existing == null) {
                foodDAO.insert(restaurantId, nameField.getText().trim(), descriptionField.getText().trim(),
                        price, imageUrlField.getText());
            } else {
                foodDAO.update(existing.getId(), nameField.getText().trim(), descriptionField.getText().trim(),
                        price, imageUrlField.getText());
            }
            messageLabel.setText("");
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not save that food item.");
        }
    }

    // ==================================================================
    // Coupons tab
    // ==================================================================

    private void setupCouponTable() {
        couponCodeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCode()));
        couponDiscountColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDiscountPercent() + "%"));
        couponRangeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStartDate() + " to " + data.getValue().getEndDate()));
        couponStatusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isCurrentlyValid() ? "Active now"
                        : (data.getValue().isActive() ? "Enabled (out of date range)" : "Disabled")));
    }

    private void loadCoupons() {
        try {
            couponTable.setItems(FXCollections.observableArrayList(couponDAO.findAll()));
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not load coupons.");
        }
    }

    @FXML
    private void onAddCoupon() {
        showCouponDialog(null);
    }

    @FXML
    private void onEditCoupon() {
        Coupon selected = couponTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a coupon first.");
            return;
        }
        showCouponDialog(selected);
    }

    @FXML
    private void onToggleCouponActive() {
        Coupon selected = couponTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a coupon first.");
            return;
        }
        try {
            couponDAO.setActive(selected.getId(), !selected.isActive());
            loadCoupons();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not update that coupon.");
        }
    }

    @FXML
    private void onDeleteCoupon() {
        Coupon selected = couponTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a coupon first.");
            return;
        }
        try {
            couponDAO.delete(selected.getId());
            loadCoupons();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not delete that coupon.");
        }
    }

    /** Add/edit form for one coupon. Passing null means "create a new one" (the code can then be chosen). */
    private void showCouponDialog(Coupon existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Coupon" : "Edit Coupon");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField codeField = new TextField(existing != null ? existing.getCode() : "");
        codeField.setDisable(existing != null); // the code is fixed once created, to keep past orders meaningful
        Spinner<Integer> discountField = new Spinner<>(1, 100, existing != null ? existing.getDiscountPercent() : 10);
        discountField.setEditable(true);
        DatePicker startPicker = new DatePicker(existing != null ? existing.getStartDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(existing != null ? existing.getEndDate() : LocalDate.now().plusMonths(1));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Code"), codeField);
        grid.addRow(1, new Label("Discount %"), discountField);
        grid.addRow(2, new Label("Start Date"), startPicker);
        grid.addRow(3, new Label("End Date"), endPicker);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        if (codeField.getText().isBlank() || startPicker.getValue() == null || endPicker.getValue() == null) {
            messageLabel.setText("Please fill in every coupon field.");
            return;
        }
        if (endPicker.getValue().isBefore(startPicker.getValue())) {
            messageLabel.setText("The end date must be on or after the start date.");
            return;
        }

        try {
            if (existing == null) {
                couponDAO.insert(codeField.getText(), discountField.getValue(), startPicker.getValue(), endPicker.getValue());
            } else {
                couponDAO.update(existing.getId(), discountField.getValue(), startPicker.getValue(), endPicker.getValue());
            }
            messageLabel.setText("");
            loadCoupons();
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not save that coupon (is the code already used?).");
        }
    }

    // ==================================================================
    // All Orders tab (tracking)
    // ==================================================================

    private void setupOrderTable() {
        orderIdColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        orderCustomerColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCustomerName()));
        orderRestaurantColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRestaurantName()));
        orderCouponColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getCouponCode() == null ? "-" : data.getValue().getCouponCode()));
        orderTotalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(PriceFormatter.format(data.getValue().getTotal())));
        orderStatusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().getLabel()));
        orderDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCreatedAt()));
    }

    private void loadOrders() {
        try {
            orderTable.setItems(FXCollections.observableArrayList(orderDAO.findAll()));
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not load orders.");
        }
    }

    // ==================================================================

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}
