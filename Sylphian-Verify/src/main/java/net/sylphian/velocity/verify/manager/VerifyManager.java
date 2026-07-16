package net.sylphian.velocity.verify.manager;

import net.sylphian.velocity.verify.VerifyVelocity;
import net.sylphian.velocity.verify.model.PlayerIdentity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sylphian.velocity.verify.api.VerifyService;
import net.sylphian.velocity.verify.api.model.VerificationResponse;
import net.sylphian.velocity.verify.model.VerificationResult;
import net.sylphian.velocity.verify.util.MessageUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the verification logic on the Velocity proxy.
 * Handles rate limiting for login attempts, cooldown management, and periodic re-verification checks.
 * Uses Caffeine for performant in-memory caching of attempts and state.
 */
public class VerifyManager {

    private final VerifyService verifyService;
    private final Map<String, Object> config;

    /** Consecutive failed verification checks per player; reaching the limit kicks the player. */
    private final Cache<UUID, Integer> uuidStrikes;
    private final Cache<UUID, Integer> uuidAttempts;
    private final Cache<String, Integer> ipAttempts;
    private final Cache<UUID, Long> uuidCooldown;
    private final Cache<String, Long> ipCooldown;

    public VerifyManager(VerifyService verifyService, Map<String, Object> config) {
        this.verifyService = verifyService;
        this.config = config;

        int attemptExpiry = configInt("attempt_expiry_minutes", 30);
        int cooldown = configInt("cooldown_minutes", 15);

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

    /**
     * Checks if a player is permitted to connect based on their verification status and rate limits.
     *
     * @param uuid the player's Mojang UUID
     * @param ip   the player's IP address
     * @return a future containing the verification result
     */
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

    /**
     * Increments failure counters and triggers cooldowns if limits are reached.
     *
     * @param uuid the player's UUID
     * @param ip   the player's IP
     */
    private void handleFailedAttempt(UUID uuid, String ip) {
        int uAttempts = uuidAttempts.get(uuid, k -> 0) + 1;
        uuidAttempts.put(uuid, uAttempts);
        if (uAttempts >= configInt("uuid_attempt_limit", 5)) {
            uuidCooldown.put(uuid, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(configInt("cooldown_minutes", 15)));
        }

        int iAttempts = ipAttempts.get(ip, k -> 0) + 1;
        ipAttempts.put(ip, iAttempts);
        if (iAttempts >= configInt("ip_attempt_limit", 10)) {
            ipCooldown.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(configInt("cooldown_minutes", 15)));
        }
    }

    /**
     * Clears all attempt tracking and cooldowns for a specific player.
     *
     * @param uuid the player's UUID
     * @param ip   the player's IP
     */
    private void resetAttempts(UUID uuid, String ip) {
        uuidAttempts.invalidate(uuid);
        ipAttempts.invalidate(ip);
        uuidCooldown.invalidate(uuid);
        ipCooldown.invalidate(ip);
    }

    /**
     * Adds a strike for a player and returns whether the max limit was reached.
     *
     * @param uuid the player's UUID
     * @return true if the strike limit was reached
     */
    public boolean addStrike(UUID uuid) {
        int strikes = uuidStrikes.get(uuid, k -> 0) + 1;
        uuidStrikes.put(uuid, strikes);
        return strikes >= configInt("max_strikes", 3);
    }

    /**
     * Reads an integer config value, tolerating any numeric type SnakeYAML produced.
     *
     * @param key          the config key
     * @param defaultValue the value to use when the key is absent
     * @return the configured integer, or {@code defaultValue} if absent
     */
    private int configInt(String key, int defaultValue) {
        return ((Number) config.getOrDefault(key, defaultValue)).intValue();
    }

    /**
     * Resets the strike counter for a player.
     *
     * @param uuid the player's UUID
     */
    public void resetStrikes(UUID uuid) {
        uuidStrikes.invalidate(uuid);
    }

    /**
     * Performs a batch re-verification check for multiple online players.
     * Used by the periodic task in {@link VerifyVelocity}.
     *
     * @param uuids the collection of player UUIDs to check
     * @return a future containing a map of results per player
     */
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
                            // Fail open for a few attempts to mitigate temporary API downtime
                            results.put(uuid, VerificationResult.allowed(null));
                        }
                        return results;
                    }

                    for (UUID uuid : uuids) {
                        VerificationResponse response = responses.get(uuid.toString());
                        if (response == null) {
                            // Player missing from the batch response: assume status hasn't changed
                            results.put(uuid, VerificationResult.allowed(null));
                            continue;
                        }

                        PlayerIdentity identity = PlayerIdentity.from(response, uuid);
                        if (response.isAllowed()) {
                            resetStrikes(uuid);
                            results.put(uuid, VerificationResult.allowed(identity));
                        } else {
                            // Kick only once the failure has repeated across multiple checks (strikes)
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

    /**
     * @param uuid the player's UUID
     * @return the current strike count, or 0 if none recorded
     */
    public int getStrikeCount(UUID uuid) {
        Integer strikes = uuidStrikes.getIfPresent(uuid);
        return strikes == null ? 0 : strikes;
    }
}
