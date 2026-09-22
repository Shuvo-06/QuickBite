package com.quickbite.quickbite.controller;

import com.quickbite.quickbite.model.FoodItem;
import com.quickbite.quickbite.model.Order;
import com.quickbite.quickbite.model.OrderItem;
import com.quickbite.quickbite.model.Restaurant;
import com.quickbite.quickbite.service.OrderService;
import com.quickbite.quickbite.service.SampleDataService;
import com.quickbite.quickbite.util.Navigator;
import com.quickbite.quickbite.util.PriceFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestaurantController {

    @FXML private Label welcomeLabel;
    @FXML private VBox restaurantList;
    @FXML private Label menuTitleLabel;
    @FXML private VBox foodList;
    @FXML private Label totalLabel;
    @FXML private Label messageLabel;

    private final SampleDataService sampleDataService = new SampleDataService();
    private final OrderService orderService = new OrderService();

    private String username;
    private Restaurant selectedRestaurant;

    // One quantity spinner per food on screen (LinkedHashMap keeps menu order)
    private final Map<FoodItem, Spinner<Integer>> quantitySpinners = new LinkedHashMap<>();

    /** Runs automatically after the FXML is loaded. */
    @FXML
    private void initialize() {
        for (Restaurant restaurant : sampleDataService.getRestaurants()) {
            restaurantList.getChildren().add(createRestaurantCard(restaurant));
        }
        updateTotal();
    }

    /** Called by Navigator right after loading, to pass the logged-in username. */
    public void setUsername(String username) {
        this.username = username;
        welcomeLabel.setText("Welcome, " + username);
    }

    // ------------------------------------------------------------------
    // Restaurants
    // ------------------------------------------------------------------

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
        // Highlight only the clicked card
        for (Node card : restaurantList.getChildren()) {
            card.getStyleClass().remove("selected");
        }
        clickedCard.getStyleClass().add("selected");

        selectedRestaurant = restaurant;
        showMenu(restaurant);
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    private void showMenu(Restaurant restaurant) {
        menuTitleLabel.setText(restaurant.getName());
        messageLabel.setText("");
        foodList.getChildren().clear();
        quantitySpinners.clear();

        for (FoodItem food : restaurant.getMenu()) {
            foodList.getChildren().add(createFoodCard(food));
        }
        updateTotal();
    }

    private HBox createFoodCard(FoodItem food) {
        Label name = new Label(food.getName());
        name.getStyleClass().add("card-title");

        Label description = new Label(food.getDescription());
        description.getStyleClass().add("card-text");

        VBox textBox = new VBox(4, name, description);

        Label price = new Label(PriceFormatter.format(food.getPrice()));
        price.getStyleClass().add("price-label");

        // Quantity picker: 0 to 10, starts at 0 (= not ordered)
        Spinner<Integer> quantity = new Spinner<>(0, 10, 0);
        quantity.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        quantity.setPrefWidth(110);
        quantity.valueProperty().addListener((observable, oldValue, newValue) -> updateTotal());
        quantitySpinners.put(food, quantity);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox card = new HBox(16, textBox, spacer, price, quantity);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("food-card");
        return card;
    }

    private void updateTotal() {
        double total = 0;
        for (Map.Entry<FoodItem, Spinner<Integer>> entry : quantitySpinners.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue().getValue();
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

        // Collect every food whose quantity is above 0
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<FoodItem, Spinner<Integer>> entry : quantitySpinners.entrySet()) {
            int quantity = entry.getValue().getValue();
            if (quantity > 0) {
                FoodItem food = entry.getKey();
                items.add(new OrderItem(food.getName(), quantity, food.getPrice()));
            }
        }

        if (items.isEmpty()) {
            messageLabel.setText("Please choose at least one food item.");
            return;
        }

        Order order = orderService.createOrder(username, selectedRestaurant, items);

        Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
        confirmation.setTitle("Order placed");
        confirmation.setHeaderText("Order #" + order.getId() + " placed successfully!");
        confirmation.setContentText("Total: " + PriceFormatter.format(order.getTotal())
                + "\nNext: your delivery status.");
        confirmation.showAndWait();

        Navigator.showDelivery(order);
    }

    @FXML
    private void onLogout() {
        Navigator.showLogin();
    }
}