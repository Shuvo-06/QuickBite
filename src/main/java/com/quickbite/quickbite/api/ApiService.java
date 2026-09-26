package com.quickbite.quickbite.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Talks to TheMealDB (https://www.themealdb.com/api.php), a free public API that needs no API key.
 * This is QuickBite's "external/public API" requirement: it supplies real dish names, categories
 * and photos as JSON for the optional "Discover a Dish" feature on the restaurant screen.
 *
 * Flow:
 *   JavaFX UI thread -> ExecutorService -> HttpClient -> JSON response -> Jackson -> MealDto
 *        -> Platform.runLater() -> update JavaFX UI
 *
 * IMPORTANT: none of QuickBite's core features (browsing, ordering, tracking) depend on this
 * class. If the API or the network is unavailable, only "Discover a Dish" is affected, and the
 * failure is reported back through onFailure instead of crashing anything.
 */
public class ApiService {

    private static final String RANDOM_MEAL_URL = "https://www.themealdb.com/api/json/v1/1/random.php";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private static final ApiService INSTANCE = new ApiService();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // A small dedicated pool for API calls, separate from OrderTrackingService's pool,
    // since the two do unrelated jobs. Daemon threads never keep the JVM alive by themselves.
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "api-worker");
        thread.setDaemon(true);
        return thread;
    });

    private ApiService() {
    }

    public static ApiService getInstance() {
        return INSTANCE;
    }

    /**
     * Fetches one random dish in the background and hands the result back on the JavaFX thread.
     * Exactly one of onSuccess/onFailure is called, always on the JavaFX Application Thread.
     */
    public void fetchRandomMeal(Consumer<MealDto> onSuccess, Consumer<String> onFailure) {
        executor.submit(() -> {
            try {
                MealDto meal = requestRandomMeal(); // runs on the "api-worker" background thread
                Platform.runLater(() -> onSuccess.accept(meal));
            } catch (Exception e) {
                // Network failure, timeout, or unexpected JSON: never crash the app for this.
                Platform.runLater(() -> onFailure.accept(describeError(e)));
            }
        });
    }

    /** Runs on a background thread. Blocking HTTP calls must never happen on the JavaFX thread. */
    private MealDto requestRandomMeal() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RANDOM_MEAL_URL))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("TheMealDB responded with HTTP " + response.statusCode());
        }

        MealResponse parsed = objectMapper.readValue(response.body(), MealResponse.class);
        if (parsed.getMeals() == null || parsed.getMeals().isEmpty()) {
            throw new IOException("TheMealDB returned no meals");
        }
        return parsed.getMeals().get(0);
    }

    private String describeError(Exception e) {
        if (e instanceof HttpTimeoutException) {
            return "The recipe service took too long to respond.";
        }
        if (e instanceof ConnectException || e instanceof IOException) {
            return "Could not reach the recipe service. Please check your internet connection.";
        }
        return "Something went wrong while fetching a dish.";
    }

    /** Called from QuickBiteApp.stop() when the application closes. */
    public void shutdown() {
        executor.shutdownNow();
    }
}
