package net.sylphian.minecraft.verify.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sylphian.minecraft.verify.api.model.ApiEnvelope;
import net.sylphian.minecraft.verify.api.model.VerificationResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * HTTP client for communicating with the Sylphian verification API.
 * Uses Java's HttpClient to perform asynchronous GET requests for player verification data.
 */
public class VerifyClient {
    /** The base URL of the API endpoint. */
    private final String baseUrl;
    /** The API key used for authentication with the XenForo API. */
    private final String apiKey;
    /** The shared HttpClient instance. */
    private final HttpClient httpClient;
    /** Gson instance for parsing JSON responses. */
    private final Gson gson;
    /** Logger for tracking API requests and errors. */
    private final Logger logger;

    /**
     * Constructs a new VerifyClient.
     *
     * @param baseUrl the API base URL
     * @param apiKey  the API authentication key
     * @param gson    the Gson instance for JSON parsing
     * @param logger  the logger instance
     */
    public VerifyClient(String baseUrl, String apiKey, Gson gson, Logger logger) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.gson = gson;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Checks the verification status of a single player.
     *
     * @param uuid the Mojang UUID of the player to check
     * @return a future containing the API envelope with verification details
     */
    public CompletableFuture<ApiEnvelope<VerificationResponse>> checkVerification(UUID uuid) {
        String url = baseUrl + "?uuid=" + uuid.toString();
        logger.info("Calling API: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "VerifyPlugin/1.0")
                .header("XF-Api-Key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // Log the response for debugging purposes
                    logger.info("API Response (" + response.statusCode() + "): " + response.body());
                    
                    // Fail if the server returns a 5xx error
                    if (response.statusCode() >= 500) {
                        throw new RuntimeException("API error: " + response.statusCode());
                    }
                    
                    try {
                        // Deserialize the JSON body into the generic ApiEnvelope
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<VerificationResponse>>() {}.getType());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to parse API response", e);
                        throw e;
                    }
                });
    }

    /**
     * Checks the verification status of multiple players in a single request.
     *
     * @param uuids a collection of player UUIDs to check
     * @return a future containing a map of UUID strings to verification details
     */
    public CompletableFuture<ApiEnvelope<Map<String, VerificationResponse>>> checkVerificationBatch(java.util.Collection<UUID> uuids) {
        // Construct query parameters for the batch request
        String query = uuids.stream()
                .map(uuid -> "uuids[]=" + uuid.toString())
                .collect(Collectors.joining("&"));
        String url = baseUrl + "?" + query;
        logger.info("Calling Batch API: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "VerifyPlugin/1.0")
                .header("XF-Api-Key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info("Batch API Response (" + response.statusCode() + "): " + response.body());
                    
                    if (response.statusCode() >= 500) {
                        throw new RuntimeException("API error: " + response.statusCode());
                    }
                    
                    try {
                        // Deserialize into a Map where keys are UUID strings
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<Map<String, VerificationResponse>>>() {}.getType());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to parse batch API response", e);
                        throw e;
                    }
                });
    }
}
