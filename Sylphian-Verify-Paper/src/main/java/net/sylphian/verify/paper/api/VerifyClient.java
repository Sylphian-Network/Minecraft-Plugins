package net.sylphian.verify.paper.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sylphian.verify.paper.api.model.ApiEnvelope;
import net.sylphian.verify.paper.api.model.VerificationResponse;

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

public class VerifyClient {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Logger logger;

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
                    logger.info("API Response (" + response.statusCode() + "): " + response.body());
                    if (response.statusCode() >= 500) {
                        throw new RuntimeException("API error: " + response.statusCode());
                    }
                    try {
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<VerificationResponse>>() {}.getType());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to parse API response", e);
                        throw e;
                    }
                });
    }

    public CompletableFuture<ApiEnvelope<Map<String, VerificationResponse>>> checkVerificationBatch(java.util.Collection<UUID> uuids) {
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
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<Map<String, VerificationResponse>>>() {}.getType());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to parse batch API response", e);
                        throw e;
                    }
                });
    }
}
