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

/**
 * Manages the verification logic on the Velocity proxy.
 * Handles rate limiting for login attempts, cooldown management, and periodic re-verification checks.
 * Uses Caffeine for performant in-memory caching of attempts and state.
 */
public class VerifyManager {
    /** The service used for API communication. */
    private final VerifyService verifyService;
    /** The configuration map. */
    private final Map<String, Object> config;

    /** Tracks consecutive API failures (strikes) per player. */
    private final Cache<UUID, Integer> uuidStrikes;
    /** Tracks failed login attempts by Mojang UUID. */
    private final Cache<UUID, Integer> uuidAttempts;
    /** Tracks failed login attempts by IP address. */
    private final Cache<String, Integer> ipAttempts;
    /** Stores the expiration time for UUID-based cooldowns. */
    private final Cache<UUID, Long> uuidCooldown;
    /** Stores the expiration time for IP-based cooldowns. */
    private final Cache<String, Long> ipCooldown;

    /**
     * Constructs a new VerifyManager.
     *
     * @param verifyService the verification service
     * @param config        the configuration map
     */
    public VerifyManager(VerifyService verifyService, Map<String, Object> config) {
        this.verifyService = verifyService;
        this.config = config;

        int attemptExpiry = (Integer) config.getOrDefault("attempt_expiry_minutes", 30);
        int cooldown = (Integer) config.getOrDefault("cooldown_minutes", 15);

        // Initialize caches with configurable expiration times
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
        // Check for active cooldowns before hitting the API
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
                // Reset rate limits on successful verification
                resetAttempts(uuid, ip);
                return VerificationResult.allowed(PlayerIdentity.from(response, uuid));
            } else {
                // Track failure and return a denied result
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
        if (uAttempts >= (Integer) config.getOrDefault("uuid_attempt_limit", 5)) {
            uuidCooldown.put(uuid, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis((Integer) config.getOrDefault("cooldown_minutes", 15)));
        }

        int iAttempts = ipAttempts.get(ip, k -> 0) + 1;
        ipAttempts.put(ip, iAttempts);
        if (iAttempts >= (Integer) config.getOrDefault("ip_attempt_limit", 10)) {
            ipCooldown.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis((Integer) config.getOrDefault("cooldown_minutes", 15)));
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
     * Adds a strike (technical failure) for a player and returns if the max limit was reached.
     *
     * @param uuid the player's UUID
     * @return true if the strike limit was reached
     */
    public boolean addStrike(UUID uuid) {
        int strikes = uuidStrikes.get(uuid, k -> 0) + 1;
        uuidStrikes.put(uuid, strikes);
        return strikes >= (Integer) config.getOrDefault("max_strikes", 3);
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
     * Used by the periodic task in {@link net.sylphian.verify.velocity.VerifyVelocity}.
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
                    
                    // Handle batch API failure
                    if (ex != null || responses == null || responses.isEmpty()) {
                        for (UUID uuid : uuids) {
                            if (Boolean.TRUE.equals(config.getOrDefault("strike_on_api_failure", true))) {
                                if (addStrike(uuid)) {
                                    // Kick player if they've reached the technical failure limit
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

                    // Process each player from the batch response
                    for (UUID uuid : uuids) {
                        VerificationResponse response = responses.get(uuid.toString());
                        if (response == null) {
                            // If player not in response, assume status hasn't changed
                            results.put(uuid, VerificationResult.allowed(null));
                            continue;
                        }

                        PlayerIdentity identity = PlayerIdentity.from(response, uuid);
                        if (response.isAllowed()) {
                            resetStrikes(uuid);
                            results.put(uuid, VerificationResult.allowed(identity));
                        } else {
                            // Enforce kicks only after multiple failed checks (strikes)
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
     * Gets the current strike count for a player.
     *
     * @param uuid the player's UUID
     * @return the strike count
     */
    public int getStrikeCount(UUID uuid) {
        Integer strikes = uuidStrikes.getIfPresent(uuid);
        return strikes == null ? 0 : strikes;
    }
}
