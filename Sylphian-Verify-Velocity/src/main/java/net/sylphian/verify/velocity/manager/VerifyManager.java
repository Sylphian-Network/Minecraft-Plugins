package net.sylphian.verify.velocity.manager;

import net.sylphian.verify.velocity.model.PlayerIdentity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sylphian.verify.velocity.api.VerifyService;
import net.sylphian.verify.velocity.api.model.VerificationResponse;
import net.sylphian.verify.velocity.model.VerificationResult;
import net.sylphian.verify.velocity.util.MessageUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VerifyManager {
    private final VerifyService verifyService;
    private final Map<String, Object> config;

    private final Cache<UUID, Integer> uuidStrikes;
    private final Cache<UUID, Integer> uuidAttempts;
    private final Cache<String, Integer> ipAttempts;
    private final Cache<UUID, Long> uuidCooldown;
    private final Cache<String, Long> ipCooldown;

    public VerifyManager(VerifyService verifyService, Map<String, Object> config) {
        this.verifyService = verifyService;
        this.config = config;

        int attemptExpiry = (Integer) config.getOrDefault("attempt_expiry_minutes", 30);
        int cooldown = (Integer) config.getOrDefault("cooldown_minutes", 15);

        this.uuidStrikes = Caffeine.newBuilder()
                .expireAfterWrite(attemptExpiry, TimeUnit.MINUTES)
                .build();

        this.uuidAttempts = Caffeine.newBuilder()
                .expireAfterWrite(attemptExpiry, TimeUnit.MINUTES)
                .build();

        this.ipAttempts = Caffeine.newBuilder()
                .expireAfterWrite(attemptExpiry, TimeUnit.MINUTES)
                .build();

        this.uuidCooldown = Caffeine.newBuilder()
                .expireAfterWrite(cooldown, TimeUnit.MINUTES)
                .build();

        this.ipCooldown = Caffeine.newBuilder()
                .expireAfterWrite(cooldown, TimeUnit.MINUTES)
                .build();
    }

    public CompletableFuture<VerificationResult> checkPlayer(UUID uuid, String ip) {
        Long uuidExpiry = uuidCooldown.getIfPresent(uuid);
        if (uuidExpiry != null && uuidExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(uuidExpiry, config)));
        }

        Long ipExpiry = ipCooldown.getIfPresent(ip);
        if (ipExpiry != null && ipExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(ipExpiry, config)));
        }

        return verifyService.checkVerification(uuid).thenApply(response -> {
            if (response.isAllowed()) {
                resetAttempts(uuid, ip);
                return VerificationResult.allowed(PlayerIdentity.from(response, uuid));
            } else {
                handleFailedAttempt(uuid, ip);
                return VerificationResult.denied(MessageUtils.buildKickMessage(response, config));
            }
        });
    }

    private void handleFailedAttempt(UUID uuid, String ip) {
        int uAttempts = uuidAttempts.get(uuid, k -> 0) + 1;
        uuidAttempts.put(uuid, uAttempts);
        if (uAttempts >= (Integer) config.getOrDefault("uuid_attempt_limit", 5)) {
            uuidCooldown.put(uuid, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis((Integer) config.getOrDefault("cooldown_minutes", 15)));
        }

        int iAttempts = ipAttempts.get(ip, k -> 0) + 1;
        ipAttempts.put(ip, iAttempts);
        if (iAttempts >= (Integer) config.getOrDefault("ip_attempt_limit", 10)) {
            ipCooldown.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis((Integer) config.getOrDefault("cooldown_minutes", 15)));
        }
    }

    private void resetAttempts(UUID uuid, String ip) {
        uuidAttempts.invalidate(uuid);
        ipAttempts.invalidate(ip);
        uuidCooldown.invalidate(uuid);
        ipCooldown.invalidate(ip);
    }

    public boolean addStrike(UUID uuid) {
        int strikes = uuidStrikes.get(uuid, k -> 0) + 1;
        uuidStrikes.put(uuid, strikes);
        return strikes >= (Integer) config.getOrDefault("max_strikes", 3);
    }

    public void resetStrikes(UUID uuid) {
        uuidStrikes.invalidate(uuid);
    }

    public CompletableFuture<Map<UUID, VerificationResult>> checkPeriodicBatch(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        return verifyService.checkVerificationBatch(uuids)
                .handle((responses, ex) -> {
                    Map<UUID, VerificationResult> results = new HashMap<>();
                    if (ex != null || responses == null || responses.isEmpty()) {
                        for (UUID uuid : uuids) {
                            if (Boolean.TRUE.equals(config.getOrDefault("strike_on_api_failure", true))) {
                                if (addStrike(uuid)) {
                                    resetStrikes(uuid);
                                    results.put(uuid, VerificationResult.denied(MessageUtils.buildReverificationFailureMessage(config)));
                                    continue;
                                }
                            }
                            results.put(uuid, VerificationResult.allowed(null));
                        }
                        return results;
                    }

                    for (UUID uuid : uuids) {
                        VerificationResponse response = responses.get(uuid.toString());
                        if (response == null) {
                            results.put(uuid, VerificationResult.allowed(null));
                            continue;
                        }

                        PlayerIdentity identity = PlayerIdentity.from(response, uuid);
                        if (response.isAllowed()) {
                            resetStrikes(uuid);
                            results.put(uuid, VerificationResult.allowed(identity));
                        } else {
                            if (addStrike(uuid)) {
                                resetStrikes(uuid);
                                results.put(uuid, VerificationResult.denied(MessageUtils.buildReverificationFailureMessage(config)));
                            } else {
                                results.put(uuid, VerificationResult.allowed(identity));
                            }
                        }
                    }
                    return results;
                });
    }

    public int getStrikeCount(UUID uuid) {
        Integer strikes = uuidStrikes.getIfPresent(uuid);
        return strikes == null ? 0 : strikes;
    }
}
