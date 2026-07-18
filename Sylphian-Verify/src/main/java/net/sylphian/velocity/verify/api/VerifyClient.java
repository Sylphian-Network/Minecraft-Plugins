package net.sylphian.velocity.verify.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sylphian.velocity.verify.api.model.ApiEnvelope;
import net.sylphian.velocity.verify.api.model.VerificationResponse;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for communicating with the Sylphian verification API from the Velocity proxy.
 * Handles asynchronous requests to fetch player link status and identity data.
 */
public class VerifyClient implements AutoCloseable {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Logger logger;
    private final Duration timeout;
    private volatile boolean logAllBatchChecks;

    /**
     * @param baseUrl        the API base URL
     * @param apiKey         the API authentication key
     * @param gson           the Gson instance
     * @param logger         the logger instance
     * @param timeoutSeconds the connect and per-request timeout, in seconds
     */
    public VerifyClient(String baseUrl, String apiKey, Gson gson, Logger logger, int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.gson = gson;
        this.logger = logger;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Toggles verbose logging of periodic batch checks. When false, a routine batch call
     * logs nothing on success; HTTP errors are always logged regardless.
     *
     * @param logAllBatchChecks true to log every batch call, false to log only failures
     */
    public void setLogAllBatchChecks(boolean logAllBatchChecks) {
        this.logAllBatchChecks = logAllBatchChecks;
    }

    /**
     * Checks the verification status of a single player.
     *
     * @param uuid the Mojang UUID of the player
     * @return a future containing the API response envelope
     */
    public CompletableFuture<ApiEnvelope<VerificationResponse>> checkVerification(UUID uuid) {
        String url = baseUrl + "?uuid=" + uuid.toString();
        logger.info("Calling API: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "VerifyPlugin/1.0")
                .header("XF-Api-Key", apiKey)
                .timeout(timeout)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info("Verify API: {} for {}", response.statusCode(), uuid);
                    if (response.statusCode() >= 400) {
                        logger.warn("Verify API returned HTTP {} for {} — check API key and endpoint URL. Body: {}",
                                response.statusCode(), uuid, response.body());
                        throw new RuntimeException("HTTP " + response.statusCode());
                    }
                    try {
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<VerificationResponse>>() {}.getType());
                    } catch (Exception e) {
                        logger.error("Failed to parse API response for {}", uuid, e);
                        throw e;
                    }
                });
    }

    /**
     * Checks the verification status of multiple players in a single batch request.
     *
     * @param uuids a collection of player UUIDs
     * @return a future containing the API envelope with a map of verification results
     */
    public CompletableFuture<ApiEnvelope<Map<String, VerificationResponse>>> checkVerificationBatch(java.util.Collection<UUID> uuids) {
        String query = uuids.stream()
                .map(uuid -> "uuids[]=" + uuid.toString())
                .collect(java.util.stream.Collectors.joining("&"));
        String url = baseUrl + "?" + query;
        if (logAllBatchChecks) {
            logger.info("Calling Batch API: {}", url);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "VerifyPlugin/1.0")
                .header("XF-Api-Key", apiKey)
                .timeout(timeout)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (logAllBatchChecks) {
                        logger.info("Verify batch API: {} for {} player(s)", response.statusCode(), uuids.size());
                    }
                    if (response.statusCode() >= 400) {
                        logger.warn("Verify batch API returned HTTP {} — check API key and endpoint URL. Body: {}",
                                response.statusCode(), response.body());
                        throw new RuntimeException("HTTP " + response.statusCode());
                    }
                    try {
                        return gson.fromJson(response.body(), new TypeToken<ApiEnvelope<Map<String, VerificationResponse>>>() {}.getType());
                    } catch (Exception e) {
                        logger.error("Failed to parse batch API response", e);
                        throw e;
                    }
                });
    }
}
