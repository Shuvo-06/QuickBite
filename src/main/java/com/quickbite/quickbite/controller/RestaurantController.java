package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.api.ApiService;
import com.quickbite.quickbite.api.MealDto;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RestaurantController {

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
    @FXML private Button discoverButton;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final OrderService orderService = new OrderService();

    private String username;
    private Restaurant selectedRestaurant;
    private Coupon appliedCoupon; // null until a valid code is applied

    // Every restaurant loaded from the database, kept so search can filter without re-querying.
    private List<Restaurant> allRestaurants = new ArrayList<>();

    // Chosen quantity per food item. Kept separate from the spinner widgets themselves,
    // so a quantity is remembered even after the food card is rebuilt by a search filter.
    private final Map<FoodItem, Integer> quantities = new LinkedHashMap<>();

    // Stored in a field so the SAME listener instance can be removed again in dispose().
    private final Consumer<Boolean> onlineListener = this::onNetworkStatusChanged;

    /** Runs automatically after the FXML is loaded. */
    @FXML
    private void initialize() {
        loadRestaurants();
        updateTotal();

        restaurantSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterRestaurants(newValue));
        foodSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterFoods(newValue));

        NetworkMonitor.getInstance().addListener(onlineListener);
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
            // findAll() only returns restaurants the admin has NOT blacklisted.
            allRestaurants = restaurantDAO.findAll();
            renderRestaurantList(allRestaurants);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
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
                card.getStyleClass().add("selected"); // keep the highlight while searching
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
        quantities.clear(); // a fresh cart for the newly chosen restaurant
        clearCoupon();
        messageLabel.setText("");
        menuTitleLabel.setText(restaurant.getName());

        if (foodSearchField.getText().isEmpty()) {
            renderFoodList(restaurant.getMenu());
        } else {
            foodSearchField.clear(); // triggers filterFoods(""), which shows the full new menu
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

        // Quantity picker: starts at whatever was chosen before (0 if never chosen)
        Spinner<Integer> quantity = new Spinner<>(0, 10, quantities.getOrDefault(food, 0));
        quantity.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        quantity.setPrefWidth(110);
        quantity.valueProperty().addListener((obs, oldValue, newValue) -> {
            quantities.put(food, newValue);
            updateTotal();
        });

        HBox card = new HBox(16, thumbnail, textBox, price, quantity);
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
        double discount = appliedCoupon != null ? subtotal * appliedCoupon.getDiscountPercent() / 100.0 : 0;
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
                couponMessageLabel.setText(coupon.getCode() + " applied: " + coupon.getDiscountPercent() + "% off");
            }
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
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

        // The order must be blocked while offline, even though the database itself is local —
        // this simulates the "cannot reach the server" behaviour the app should show offline.
        if (!NetworkMonitor.getInstance().isOnline()) {
            Alert offlineAlert = new Alert(Alert.AlertType.ERROR,
                    "Cannot connect to server. Please check your internet connection and try again.");
            offlineAlert.setHeaderText("You're offline");
            offlineAlert.showAndWait();
            return;
        }

        Order order;
        try {
            String couponCode = appliedCoupon != null ? appliedCoupon.getCode() : null;
            order = orderService.createOrder(username, selectedRestaurant, items, couponCode);
        } catch (SQLException e) {
            e.printStackTrace(); // details for the developer console only
            messageLabel.setText("Could not save your order. Please try again.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
        confirmation.setTitle("Order placed");
        confirmation.setHeaderText("Order #" + order.getId() + " placed successfully!");
        confirmation.setContentText("Total: " + PriceFormatter.format(order.getTotal())
                + "\nWaiting for " + selectedRestaurant.getName() + " to confirm your order.");
        confirmation.showAndWait();

        // Tracking does NOT start yet: the restaurant must Accept it first (see the dashboard).
        Navigator.showDelivery(order);
    }

    // ------------------------------------------------------------------
    // Phase 8: external API (TheMealDB) - JSON, HttpClient, concurrency
    // ------------------------------------------------------------------

    /**
     * Optional feature: fetches a random real dish (name, category, photo) from a public API.
     * Runs entirely on a background thread pool; the rest of the app works fine even if this fails.
     */
    @FXML
    private void onDiscoverDish() {
        discoverButton.setDisable(true);
        messageLabel.setText("Looking for a dish suggestion...");

        ApiService.getInstance().fetchRandomMeal(
                this::showMealDialog,
                error -> {
                    discoverButton.setDisable(false);
                    messageLabel.setText(error);
                }
        );
    }

    /** Called on the JavaFX thread once the API call succeeds. */
    private void showMealDialog(MealDto meal) {
        discoverButton.setDisable(false);
        messageLabel.setText("");

        VBox content = new VBox(10);
        content.setPrefWidth(360);

        if (meal.getImageUrl() != null && !meal.getImageUrl().isBlank()) {
            ImageView imageView = new ImageView(new Image(meal.getImageUrl(), 320, 200, true, true, true));
            content.getChildren().add(imageView);
        }

        Label category = new Label("Category: " + meal.getCategory() + "   |   Origin: " + meal.getArea());
        category.getStyleClass().add("card-text");
        category.setWrapText(true);

        Label instructions = new Label(shorten(meal.getInstructions(), 400));
        instructions.setWrapText(true);
        instructions.getStyleClass().add("card-text");

        content.getChildren().addAll(category, instructions);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Discover a Dish");
        alert.setHeaderText(meal.getName());
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "...";
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
