package com.quickbite.quickbite.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.quickbite.service.Shutdownable;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Talks to the USDA FoodData Central API (https://fdc.nal.usda.gov) to look up nutrition facts.
 *
 * Flow:
 *   JavaFX UI thread -> ExecutorService -> HttpClient -> JSON response -> Jackson -> NutritionInfo
 *        -> Platform.runLater() -> update JavaFX UI
 *
 * Nothing else in QuickBite depends on this class: if the network or the API is unavailable,
 * only the nutrition lookup fails, with a plain-English message.
 *
 * The API key is NEVER hard-coded (it would end up on GitHub). It is read from the USDA_API_KEY
 * environment variable, or from a git-ignored quickbite.properties file (usda.api.key=...),
 * falling back to USDA's shared, rate-limited DEMO_KEY.
 */
public class ApiService implements Shutdownable {

    private static final String SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private static final ApiService INSTANCE = new ApiService();

    private final String apiKey = loadApiKey();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // A small dedicated pool for API calls. Daemon threads never keep the JVM alive by themselves.
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

    /** A failure whose message is already safe and friendly to show the user. */
    private static class ApiException extends IOException {
        ApiException(String message) {
            super(message);
        }
    }

    /**
     * Looks up nutrition facts for a food name in the background and hands the result back on the
     * JavaFX thread. Exactly one of onSuccess/onFailure is called, always on the JavaFX thread.
     */
    public void fetchNutrition(String foodName, Consumer<NutritionInfo> onSuccess, Consumer<String> onFailure) {
        executor.submit(() -> {
            try {
                NutritionInfo info = requestNutrition(foodName); // runs on the "api-worker" thread
                if (info == null) {
                    Platform.runLater(() -> onFailure.accept("USDA has no nutrition data for \"" + foodName + "\"."));
                } else {
                    Platform.runLater(() -> onSuccess.accept(info));
                }
            } catch (Exception e) {
                Platform.runLater(() -> onFailure.accept(describeError(e)));
            }
        });
    }

    /** Runs on a background thread. Returns null when USDA has no match. */
    private NutritionInfo requestNutrition(String foodName) throws IOException, InterruptedException {
        // First try the generic, lab-analysed food types (much cleaner matches than branded
        // products); if nothing matches, retry across every data type.
        for (String dataTypes : new String[] { "Foundation,SR Legacy", null }) {
            FdcSearchResponse response = search(foodName, dataTypes);
            if (response.getFoods() != null && !response.getFoods().isEmpty()) {
                return NutritionInfo.fromFood(response.getFoods().get(0));
            }
        }
        return null;
    }

    private FdcSearchResponse search(String foodName, String dataTypes) throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder(SEARCH_URL)
                .append("?api_key=").append(encode(apiKey))
                .append("&query=").append(encode(foodName))
                .append("&pageSize=1");
        if (dataTypes != null) {
            url.append("&dataType=").append(encode(dataTypes));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        switch (response.statusCode()) {
            case 200 -> { }
            case 401, 403 -> throw new ApiException("The USDA API key was rejected. Check your USDA_API_KEY setting.");
            case 429 -> throw new ApiException("USDA's rate limit was reached. Please try again in a minute.");
            default -> throw new IOException("USDA responded with HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), FdcSearchResponse.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String loadApiKey() {
        String fromEnvironment = System.getenv("USDA_API_KEY");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }

        Path file = Path.of("quickbite.properties");
        if (Files.exists(file)) {
            Properties properties = new Properties();
            try (var in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                // unreadable file: fall through to the demo key
            }
            String key = properties.getProperty("usda.api.key");
            if (key != null && !key.isBlank()) {
                return key.trim();
            }
        }
        return "DEMO_KEY";
    }

    private String describeError(Exception e) {
        if (e instanceof ApiException) {
            return e.getMessage();
        }
        if (e instanceof HttpTimeoutException) {
            return "The nutrition service took too long to respond.";
        }
        return "Could not get nutrition data right now. Check your internet connection.";
    }

    /** Called from QuickBiteApp.stop() when the application closes. */
    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}