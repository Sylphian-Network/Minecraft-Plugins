package net.sylphian.verify.paper.api;

import net.sylphian.verify.paper.api.model.VerificationReason;
import net.sylphian.verify.paper.api.model.VerificationResponse;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VerifyService {
    private final VerifyClient client;

    public VerifyService(VerifyClient client) {
        this.client = client;
    }

    public CompletableFuture<VerificationResponse> checkVerification(UUID uuid) {
        return client.checkVerification(uuid)
                .thenApply(envelope -> {
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

    public CompletableFuture<Map<String, VerificationResponse>> checkVerificationBatch(java.util.Collection<UUID> uuids) {
        return client.checkVerificationBatch(uuids)
                .thenApply(envelope -> {
                    if (envelope == null || envelope.isFailed() || envelope.getData() == null) {
                        return Collections.<String, VerificationResponse>emptyMap();
                    }
                    return envelope.getData();
                })
                .exceptionally(ex -> Collections.emptyMap());
    }

    private VerificationResponse createErrorResponse(VerificationReason reason) {
        return new VerificationResponse(false, reason);
    }
}
