package net.sylphian.verify.velocity.api;

import net.sylphian.verify.velocity.api.model.VerificationReason;
import net.sylphian.verify.velocity.api.model.VerificationResponse;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * High-level service for handling player verification on Velocity.
 * Provides abstraction over the {@link VerifyClient} and handles error mapping.
 */
public class VerifyService {
    /** The underlying API client. */
    private final VerifyClient client;

    /**
     * Constructs a new VerifyService.
     *
     * @param client the VerifyClient to use for API calls
     */
    public VerifyService(VerifyClient client) {
        this.client = client;
    }

    /**
     * Checks the verification status of a player and maps API outcomes to response objects.
     *
     * @param uuid the player's Mojang UUID
     * @return a future containing the verification response
     */
    public CompletableFuture<VerificationResponse> checkVerification(UUID uuid) {
        return client.checkVerification(uuid)
                .thenApply(envelope -> {
                    // Map various failure states to internal VerificationReason constants
                    if (envelope == null) {
                        return createErrorResponse(VerificationReason.API_ERROR);
                    }
                    if (envelope.isFailed()) {
                        return createErrorResponse(VerificationReason.API_FAILURE_NO_MESSAGE);
                    }
                    if (envelope.getData() == null) {
                        return createErrorResponse(VerificationReason.API_SUCCESS_NO_DATA);
                    }
                    return envelope.getData();
                })
                .exceptionally(ex -> createErrorResponse(VerificationReason.API_ERROR));
    }

    /**
     * Checks the verification status of multiple players.
     *
     * @param uuids a collection of player UUIDs
     * @return a future containing a map of UUID strings to verification responses
     */
    public CompletableFuture<Map<String, VerificationResponse>> checkVerificationBatch(java.util.Collection<UUID> uuids) {
        return client.checkVerificationBatch(uuids)
                .thenApply(envelope -> {
                    // Returns an empty map if the request fails
                    if (envelope == null || envelope.isFailed() || envelope.getData() == null) {
                        return Collections.<String, VerificationResponse>emptyMap();
                    }
                    return envelope.getData();
                })
                .exceptionally(ex -> Collections.emptyMap());
    }

    /**
     * Helper method to create a failed verification response with a specific reason.
     *
     * @param reason the reason for the failure
     * @return a VerificationResponse object
     */
    private VerificationResponse createErrorResponse(VerificationReason reason) {
        return new VerificationResponse(false, reason);
    }
}
