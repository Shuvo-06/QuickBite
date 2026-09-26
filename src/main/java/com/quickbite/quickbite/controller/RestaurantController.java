package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.dao.RestaurantDAO;
import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.service.OrderService;
import com.quickbite.quickbite.util.FoodIconUtil;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
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

public class RestaurantController {

    @FXML private Label welcomeLabel;
    @FXML private TextField restaurantSearchField;
    @FXML private VBox restaurantList;
    @FXML private Label menuTitleLabel;
    @FXML private TextField foodSearchField;
    @FXML private VBox foodList;
    @FXML private Label totalLabel;
    @FXML private Label messageLabel;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final OrderService orderService = new OrderService();

    private String username;
    private Restaurant selectedRestaurant;

    // Every restaurant loaded from the database, kept so search can filter without re-querying.
    private List<Restaurant> allRestaurants = new ArrayList<>();

    // Chosen quantity per food item. Kept separate from the spinner widgets themselves,
    // so a quantity is remembered even after the food card is rebuilt by a search filter.
    private final Map<FoodItem, Integer> quantities = new LinkedHashMap<>();

    @FXML
    private void onOrderHistory() {
        Navigator.showOrderHistory(username);
    }

    /** Runs automatically after the FXML is loaded. */
    @FXML
    private void initialize() {
        loadRestaurants();
        updateTotal();

        restaurantSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterRestaurants(newValue));
        foodSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterFoods(newValue));
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
            VBox card = createRestaurantCard(restaurant);
            if (selectedRestaurant != null && selectedRestaurant.getId() == restaurant.getId()) {
                card.getStyleClass().add("selected"); // keep the highlight while searching
            }
            restaurantList.getChildren().add(card);
        }
    }

    private VBox createRestaurantCard(Restaurant restaurant) {
        Label name = new Label(restaurant.getName());
        name.getStyleClass().add("card-title");

        Label description = new Label(restaurant.getDescription());
        description.getStyleClass().add("card-text");
        description.setWrapText(true);

        Label rating = new Label(String.valueOf(restaurant.getRating()), new FontIcon("fas-star"));
        rating.getStyleClass().add("rating-label");
        ((FontIcon) rating.getGraphic()).getStyleClass().add("star-icon");

        VBox card = new VBox(6, name, description, rating);
        card.getStyleClass().add("restaurant-card");
        card.setOnMouseClicked(event -> selectRestaurant(restaurant, card));
        return card;
    }

    private void selectRestaurant(Restaurant restaurant, VBox clickedCard) {
        for (Node card : restaurantList.getChildren()) {
            card.getStyleClass().remove("selected");
        }
        clickedCard.getStyleClass().add("selected");

        selectedRestaurant = restaurant;
        quantities.clear();
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
        FontIcon icon = new FontIcon(FoodIconUtil.iconFor(food.getName()));
        icon.getStyleClass().add("food-icon");

        Label name = new Label(food.getName(), icon);
        name.getStyleClass().add("card-title");
        name.setGraphicTextGap(8);

        Label description = new Label(food.getDescription());
        description.getStyleClass().add("card-text");

        VBox textBox = new VBox(4, name, description);

        Label price = new Label(PriceFormatter.format(food.getPrice()));
        price.getStyleClass().add("price-label");

        Spinner<Integer> quantity = new Spinner<>(0, 10, quantities.getOrDefault(food, 0));
        quantity.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        quantity.setPrefWidth(110);
        quantity.valueProperty().addListener((obs, oldValue, newValue) -> {
            quantities.put(food, newValue);
            updateTotal();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox card = new HBox(16, textBox, spacer, price, quantity);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("food-card");
        return card;
    }

    private void updateTotal() {
        double total = 0;
        if (selectedRestaurant != null) {
            for (FoodItem food : selectedRestaurant.getMenu()) {
                total += food.getPrice() * quantities.getOrDefault(food, 0);
            }
        }
        totalLabel.setText("Total: " + PriceFormatter.format(total));
    }

    // ------------------------------------------------------------------
    // Actions
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

        Order order;
        try {
            order = orderService.createOrder(username, selectedRestaurant, items);
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

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}