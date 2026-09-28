package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.api.ApiService;
import com.quickbite.quickbite.api.NutritionInfo;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import com.quickbite.quickbite.dao.RestaurantDAO;
import com.quickbite.quickbite.model.Coupon;
import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.service.NetworkMonitor;
import com.quickbite.quickbite.service.OrderService;
import com.quickbite.quickbite.util.FoodIconUtil;
import com.quickbite.quickbite.util.FoodImageUtil;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.concurrent.ThreadLocalRandom;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RestaurantController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidePanel;

    @FXML private Label offlineBar;
    @FXML private Label welcomeLabel;
    @FXML private TextField restaurantSearchField;
    @FXML private VBox restaurantList;
    @FXML private Label menuTitleLabel;
    @FXML private TextField foodSearchField;
    @FXML private VBox foodList;
    @FXML private TextField couponField;
    @FXML private Label couponMessageLabel;
    @FXML private Label totalLabel;
    @FXML private Label messageLabel;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final OrderService orderService = new OrderService();

    private String username;
    private Restaurant selectedRestaurant;
    private Coupon appliedCoupon;

    private List<Restaurant> allRestaurants = new ArrayList<>();

    private final Map<FoodItem, Integer> quantities = new LinkedHashMap<>();

    private final Consumer<Boolean> onlineListener = this::onNetworkStatusChanged;

    /** Runs automatically after the FXML is loaded. */
    @FXML
    private void initialize() {
        loadRestaurants();
        updateTotal();

        restaurantSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterRestaurants(newValue));
        foodSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterFoods(newValue));

        NetworkMonitor.getInstance().addListener(onlineListener);

        sidePanel.prefWidthProperty().bind(
                Bindings.max(260, Bindings.min(420, rootPane.widthProperty().multiply(0.25)))
        );
    }

    /** Called by Navigator when this window is hidden or replaced. */
    public void dispose() {
        NetworkMonitor.getInstance().removeListener(onlineListener);
    }

    /** Called on the JavaFX thread by NetworkMonitor whenever connectivity changes. */
    private void onNetworkStatusChanged(boolean online) {
        offlineBar.setVisible(!online);
        offlineBar.setManaged(!online);
    }

    /** Called by Navigator right after loading, to pass the logged-in username. */
    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Welcome, " + username);
    }

    // ------------------------------------------------------------------
    // Restaurants
    // ------------------------------------------------------------------

    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.findAll();
            renderRestaurantList(allRestaurants);
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not load restaurants from the database.");
        }
    }

    /** Shows only restaurants whose name or description contains the search text. */
    private void filterRestaurants(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Restaurant> filtered = new ArrayList<>();

        for (Restaurant restaurant : allRestaurants) {
            if (needle.isEmpty()
                    || restaurant.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || restaurant.getDescription().toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(restaurant);
            }
        }

        renderRestaurantList(filtered);
    }

    private void renderRestaurantList(List<Restaurant> restaurants) {
        restaurantList.getChildren().clear();

        if (restaurants.isEmpty()) {
            Label empty = new Label("No restaurants match your search.");
            empty.getStyleClass().add("card-text");
            restaurantList.getChildren().add(empty);
            return;
        }

        for (Restaurant restaurant : restaurants) {
            HBox card = createRestaurantCard(restaurant);

            if (selectedRestaurant != null && selectedRestaurant.getId() == restaurant.getId()) {
                card.getStyleClass().add("selected");
            }

            restaurantList.getChildren().add(card);
        }
    }

    private HBox createRestaurantCard(Restaurant restaurant) {
        ImageView thumbnail = new ImageView(FoodImageUtil.imageForRestaurant(restaurant.getImageUrl()));
        thumbnail.setFitWidth(56);
        thumbnail.setFitHeight(56);
        thumbnail.setPreserveRatio(false);

        Label name = new Label(restaurant.getName());
        name.getStyleClass().add("card-title");

        Label description = new Label(restaurant.getDescription());
        description.getStyleClass().add("card-text");
        description.setWrapText(true);

        Label rating = new Label(String.valueOf(restaurant.getRating()), new FontIcon("fas-star"));
        rating.getStyleClass().add("rating-label");
        ((FontIcon) rating.getGraphic()).getStyleClass().add("star-icon");

        VBox textBox = new VBox(4, name, description, rating);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox card = new HBox(12, thumbnail, textBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("restaurant-card");
        card.setOnMouseClicked(event -> selectRestaurant(restaurant, card));

        return card;
    }

    private void selectRestaurant(Restaurant restaurant, HBox clickedCard) {
        for (Node card : restaurantList.getChildren()) {
            card.getStyleClass().remove("selected");
        }

        clickedCard.getStyleClass().add("selected");

        selectedRestaurant = restaurant;
        quantities.clear();
        clearCoupon();
        messageLabel.setText("");
        menuTitleLabel.setText(restaurant.getName());

        if (foodSearchField.getText().isEmpty()) {
            renderFoodList(restaurant.getMenu());
        } else {
            foodSearchField.clear();
        }

        updateTotal();
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    /** Shows only foods (of the selected restaurant) whose name or description contains the search text. */
    private void filterFoods(String query) {
        if (selectedRestaurant == null) {
            return;
        }

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<FoodItem> filtered = new ArrayList<>();

        for (FoodItem food : selectedRestaurant.getMenu()) {
            if (needle.isEmpty()
                    || food.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || food.getDescription().toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(food);
            }
        }

        renderFoodList(filtered);
    }

    private void renderFoodList(List<FoodItem> items) {
        foodList.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("No food items match your search.");
            empty.getStyleClass().add("card-text");
            foodList.getChildren().add(empty);
            return;
        }

        for (FoodItem food : items) {
            foodList.getChildren().add(createFoodCard(food));
        }
    }

    private HBox createFoodCard(FoodItem food) {
        ImageView thumbnail = new ImageView(FoodImageUtil.imageForFood(food.getName(), food.getImageUrl()));
        thumbnail.setFitWidth(64);
        thumbnail.setFitHeight(64);
        thumbnail.setPreserveRatio(false);

        FontIcon icon = new FontIcon(FoodIconUtil.iconFor(food.getName()));
        icon.getStyleClass().add("food-icon");

        Label name = new Label(food.getName(), icon);
        name.getStyleClass().add("card-title");
        name.setGraphicTextGap(8);

        Label description = new Label(food.getDescription());
        description.getStyleClass().add("card-text");

        VBox textBox = new VBox(4, name, description);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label price = new Label(PriceFormatter.format(food.getPrice()));
        price.getStyleClass().add("price-label");

        FontIcon infoIcon = new FontIcon("fas-info-circle");
        infoIcon.getStyleClass().add("nutrition-icon");
        Button nutritionButton = new Button();
        nutritionButton.setGraphic(infoIcon);
        nutritionButton.getStyleClass().add("icon-button");
        nutritionButton.setTooltip(new Tooltip("Nutrition facts (USDA)"));
        nutritionButton.setOnAction(e -> showNutrition(food, nutritionButton));

        Spinner<Integer> quantity = new Spinner<>(0, 10, quantities.getOrDefault(food, 0));
        quantity.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        quantity.setPrefWidth(110);

        quantity.valueProperty().addListener((obs, oldValue, newValue) -> {
            quantities.put(food, newValue);
            updateTotal();
        });

        HBox card = new HBox(16, thumbnail, textBox, price, nutritionButton, quantity);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("food-card");

        return card;
    }

    private double subtotal() {
        double subtotal = 0;

        if (selectedRestaurant != null) {
            for (FoodItem food : selectedRestaurant.getMenu()) {
                subtotal += food.getPrice() * quantities.getOrDefault(food, 0);
            }
        }

        return subtotal;
    }

    private void updateTotal() {
        double subtotal = subtotal();
        double discount = appliedCoupon != null
                ? subtotal * appliedCoupon.getDiscountPercent() / 100.0
                : 0;

        double total = Math.max(0, subtotal - discount);
        totalLabel.setText("Total: " + PriceFormatter.format(total));
    }

    // ------------------------------------------------------------------
    // Coupon
    // ------------------------------------------------------------------

    @FXML
    private void onApplyCoupon() {
        String code = couponField.getText();

        if (code == null || code.isBlank()) {
            couponMessageLabel.setText("Enter a coupon code first.");
            return;
        }

        try {
            Coupon coupon = orderService.findValidCoupon(code);

            if (coupon == null) {
                appliedCoupon = null;
                couponMessageLabel.getStyleClass().setAll("error-label");
                couponMessageLabel.setText("That coupon code is invalid or has expired.");
            } else {
                appliedCoupon = coupon;
                couponMessageLabel.getStyleClass().setAll("info-label");
                couponMessageLabel.setText(
                        coupon.getCode() + " applied: "
                                + coupon.getDiscountPercent() + "% off"
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            couponMessageLabel.getStyleClass().setAll("error-label");
            couponMessageLabel.setText("Could not check that coupon right now.");
        }

        updateTotal();
    }

    private void clearCoupon() {
        appliedCoupon = null;
        couponField.clear();
        couponMessageLabel.setText("");
    }

    // ------------------------------------------------------------------
    // Ordering
    // ------------------------------------------------------------------

    @FXML
    private void onPlaceOrder() {
        if (selectedRestaurant == null) {
            messageLabel.setText("Please select a restaurant first.");
            return;
        }

        List<OrderItem> items = new ArrayList<>();

        for (FoodItem food : selectedRestaurant.getMenu()) {
            int quantity = quantities.getOrDefault(food, 0);

            if (quantity > 0) {
                items.add(new OrderItem(food.getName(), quantity, food.getPrice()));
            }
        }

        if (items.isEmpty()) {
            messageLabel.setText("Please choose at least one food item.");
            return;
        }

        if (!NetworkMonitor.getInstance().isOnline()) {
            Alert offlineAlert = new Alert(
                    Alert.AlertType.ERROR,
                    "Cannot connect to server. Please check your internet connection and try again."
            );

            offlineAlert.setHeaderText("You're offline");
            offlineAlert.showAndWait();
            return;
        }

        Order order;

        try {
            String couponCode = appliedCoupon != null ? appliedCoupon.getCode() : null;
            order = orderService.createOrder(
                    username,
                    selectedRestaurant,
                    items,
                    couponCode
            );
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Could not save your order. Please try again.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
        confirmation.setTitle("Order placed");
        confirmation.setHeaderText("Order #" + order.getId() + " placed successfully!");
        confirmation.setContentText(
                "Total: " + PriceFormatter.format(order.getTotal())
                        + "\nWaiting for " + selectedRestaurant.getName()
                        + " to confirm your order."
        );

        confirmation.showAndWait();

        Navigator.showDelivery(order);
    }

    // ------------------------------------------------------------------
    // Pick a Dish for Me: a quick recommendation from the CURRENT menu data (no network at all)
    // ------------------------------------------------------------------

    /**
     * Randomly picks a dish from the selected restaurant's menu (or a random restaurant with a
     * non-empty menu, if none is selected yet), switches to it if needed, and adds one to the cart.
     * This is a purely local, synchronous operation over data already loaded from SQLite.
     */
    @FXML
    private void onPickDishForMe() {
        List<Restaurant> candidates = (selectedRestaurant != null && !selectedRestaurant.getMenu().isEmpty())
                ? List.of(selectedRestaurant)
                : allRestaurants.stream().filter(r -> !r.getMenu().isEmpty()).toList();

        if (candidates.isEmpty()) {
            messageLabel.setText("No menu items available to pick from right now.");
            return;
        }

        Restaurant restaurant = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        switchToRestaurantIfNeeded(restaurant);

        List<FoodItem> menu = restaurant.getMenu();
        FoodItem picked = menu.get(ThreadLocalRandom.current().nextInt(menu.size()));
        quantities.put(picked, quantities.getOrDefault(picked, 0) + 1);

        if (!foodSearchField.getText().isEmpty()) {
            foodSearchField.clear();
        }
        renderFoodList(menu); // rebuild the cards so the spinner reflects the new quantity
        updateTotal();
        messageLabel.setText("");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pick of the Day");
        alert.setHeaderText("We picked: " + picked.getName());
        alert.setContentText("From " + restaurant.getName()
                + ". We've added 1 to your cart — feel free to change the quantity.");
        alert.showAndWait();
    }

    /** Switches the menu panel to a different restaurant, same as clicking its card, unless it's already showing. */
    private void switchToRestaurantIfNeeded(Restaurant restaurant) {
        if (selectedRestaurant != null && selectedRestaurant.getId() == restaurant.getId()) {
            return;
        }
        selectedRestaurant = restaurant;
        quantities.clear();
        clearCoupon();
        menuTitleLabel.setText(restaurant.getName());
        filterRestaurants(restaurantSearchField.getText()); // redraws the sidebar so the pick is highlighted
    }

    // ------------------------------------------------------------------
    // Nutrition facts: external API (USDA FoodData Central) - JSON, HttpClient, concurrency
    // ------------------------------------------------------------------

    private void showNutrition(FoodItem food, Button sourceButton) {
        sourceButton.setDisable(true);
        messageLabel.setText("Looking up nutrition facts for " + food.getName() + "...");

        // The HTTP call and JSON parsing run on a background thread; these callbacks come back
        // on the JavaFX thread, so they may touch the UI directly.
        ApiService.getInstance().fetchNutrition(
                food.getName(),
                info -> {
                    sourceButton.setDisable(false);
                    messageLabel.setText("");
                    showNutritionDialog(food, info);
                },
                error -> {
                    sourceButton.setDisable(false);
                    messageLabel.setText(error);
                });
    }

    private void showNutritionDialog(FoodItem food, NutritionInfo info) {
        String[][] rows = {
                {"Calories", NutritionInfo.describe(info.getCalories(), "kcal")},
                {"Protein", NutritionInfo.describe(info.getProtein(), "g")},
                {"Fat", NutritionInfo.describe(info.getFat(), "g")},
                {"Carbohydrates", NutritionInfo.describe(info.getCarbs(), "g")},
                {"Sugars", NutritionInfo.describe(info.getSugar(), "g")},
                {"Fiber", NutritionInfo.describe(info.getFiber(), "g")},
                {"Sodium", NutritionInfo.describe(info.getSodiumMg(), "mg")}
        };

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        for (int i = 0; i < rows.length; i++) {
            Label name = new Label(rows[i][0]);
            name.getStyleClass().add("card-text");
            Label value = new Label(rows[i][1]);
            value.getStyleClass().add("card-title");
            grid.addRow(i, name, value);
        }

        Label note = new Label("Closest USDA match: " + info.getMatchedName()
                + "\nValues are per 100 g of a generic food, so they approximate this dish rather than measure it.");
        note.setWrapText(true);
        note.getStyleClass().add("card-text");

        VBox content = new VBox(12, grid, note);
        content.setPrefWidth(380);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nutrition facts");
        alert.setHeaderText(food.getName() + " (per 100 g)");
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    @FXML
    private void onOrderHistory() {
        Navigator.showOrderHistory(username);
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}