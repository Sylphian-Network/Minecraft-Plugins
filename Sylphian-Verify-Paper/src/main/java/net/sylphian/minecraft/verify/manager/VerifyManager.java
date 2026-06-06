package net.sylphian.minecraft.verify.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sylphian.minecraft.verify.api.VerifyService;
import net.sylphian.minecraft.verify.api.model.VerificationResponse;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.minecraft.verify.model.PlayerIdentity;
import net.sylphian.minecraft.verify.model.VerificationResult;
import net.sylphian.minecraft.verify.util.MessageUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the verification lifecycle for players, including rate limiting and cooldowns.
 * Uses Caffeine caches to track login attempts and enforce security policies defined in the config.
 */
public class VerifyManager {
    /** The service used to perform actual API verification checks. */
    private final VerifyService verifyService;
    /** The plugin configuration. */
    private final FileConfiguration config;

    /** Tracks number of failed attempts by Mojang UUID. */
    private final Cache<UUID, Integer> uuidAttempts;
    /** Tracks number of failed attempts by IP address. */
    private final Cache<String, Integer> ipAttempts;
    /** Stores the expiration timestamp of a cooldown for a specific UUID. */
    private final Cache<UUID, Long> uuidCooldown;
    /** Stores the expiration timestamp of a cooldown for a specific IP. */
    private final Cache<String, Long> ipCooldown;
    /** Tracks consecutive API failures (strikes) to detect service outages. */
    private final Cache<UUID, Integer> strikes;

    /**
     * Constructs a new VerifyManager and initializes internal caches.
     *
     * @param verifyService the verification service
     * @param config        the plugin configuration
     */
    public VerifyManager(VerifyService verifyService, FileConfiguration config) {
        this.verifyService = verifyService;
        this.config = config;

        // Initialize caches with expiration settings from config
        this.uuidAttempts = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();

        this.ipAttempts = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();

        this.uuidCooldown = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("cooldown_minutes", 15), TimeUnit.MINUTES)
                .build();

        this.ipCooldown = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("cooldown_minutes", 15), TimeUnit.MINUTES)
                .build();

        this.strikes = Caffeine.newBuilder()
                .expireAfterWrite(config.getInt("attempt_expiry_minutes", 30), TimeUnit.MINUTES)
                .build();
    }

    /**
     * Performs a verification check for a player, considering rate limits and cooldowns.
     *
     * @param uuid the player's Mojang UUID
     * @param ip   the player's IP address
     * @return a future containing the verification result
     */
    public CompletableFuture<VerificationResult> checkPlayer(UUID uuid, String ip) {
        // Check if the UUID is currently on cooldown
        Long uuidExpiry = uuidCooldown.getIfPresent(uuid);
        if (uuidExpiry != null && uuidExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(uuidExpiry, config), null));
        }

        // Check if the IP address is currently on cooldown
        Long ipExpiry = ipCooldown.getIfPresent(ip);
        if (ipExpiry != null && ipExpiry > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(VerificationResult.denied(MessageUtils.buildCooldownMessage(ipExpiry, config), null));
        }

        // Proceed to API check
        return verifyService.checkVerification(uuid).thenApply(response -> {
            PlayerIdentity identity = PlayerIdentity.from(response, uuid);
            if (response.isAllowed()) {
                // Success: reset rate limit counters
                resetAttempts(uuid, ip);
                return VerificationResult.allowed(identity);
            } else {
                // Failure: increment attempt counters and possibly trigger cooldown
                handleFailedAttempt(uuid, ip);
                return VerificationResult.denied(MessageUtils.buildKickMessage(response, config), identity);
            }
        });
    }

    /**
     * Increments failure counters and applies cooldowns if limits are reached.
     *
     * @param uuid the player UUID
     * @param ip   the player IP
     */
    private void handleFailedAttempt(UUID uuid, String ip) {
        // Update UUID-based attempts
        int uAttempts = uuidAttempts.get(uuid, k -> 0) + 1;
        uuidAttempts.put(uuid, uAttempts);
        if (uAttempts >= config.getInt("uuid_attempt_limit", 5)) {
            uuidCooldown.put(uuid, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.getInt("cooldown_minutes", 15)));
        }

        // Update IP-based attempts
        int iAttempts = ipAttempts.get(ip, k -> 0) + 1;
        ipAttempts.put(ip, iAttempts);
        if (iAttempts >= config.getInt("ip_attempt_limit", 10)) {
            ipCooldown.put(ip, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.getInt("cooldown_minutes", 15)));
        }
    }

    /**
     * Clears all failure tracking and cooldowns for a specific player identity.
     *
     * @param uuid the player UUID
     * @param ip   the player IP
     */
    private void resetAttempts(UUID uuid, String ip) {
        uuidAttempts.invalidate(uuid);
        ipAttempts.invalidate(ip);
        uuidCooldown.invalidate(uuid);
        ipCooldown.invalidate(ip);
    }

    /**
     * Records a 'strike' (technical failure) for a player and checks if the limit is reached.
     *
     * @param uuid the player's UUID
     * @return true if the strike limit was reached, false otherwise
     */
    public boolean addStrike(UUID uuid) {
        int s = strikes.get(uuid, k -> 0) + 1;
        strikes.put(uuid, s);
        return s >= config.getInt("max_strikes", 3);
    }

    /**
     * Resets the strike counter for a player.
     *
     * @param uuid the player's UUID
     */
    public void resetStrikes(UUID uuid) {
        strikes.invalidate(uuid);
    }

    /**
     * Performs a batch re-verification check for multiple online players.
     * Used by the periodic task in {@link net.sylphian.minecraft.verify.VerifyPaper}.
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

                    // Handle batch API failure — fail open with strike tracking
                    if (ex != null || responses == null || responses.isEmpty()) {
                        for (UUID uuid : uuids) {
                            if (config.getBoolean("strike_on_api_failure", true)) {
                                if (addStrike(uuid)) {
                                    resetStrikes(uuid);
                                    results.put(uuid, VerificationResult.denied(
                                            MessageUtils.buildReverificationFailureMessage(config), null));
                                    continue;
                                }
                            }
                            results.put(uuid, VerificationResult.allowed(null));
                        }
                        return results;
                    }

                    // Process each player from the batch response
                    for (UUID uuid : uuids) {
                        VerificationResponse response = responses.get(uuid.toString());
                        if (response == null) {
                            // Not in response — assume status unchanged
                            results.put(uuid, VerificationResult.allowed(null));
                            continue;
                        }

                        PlayerIdentity identity = PlayerIdentity.from(response, uuid);
                        if (response.isAllowed()) {
                            resetStrikes(uuid);
                            results.put(uuid, VerificationResult.allowed(identity));
                        } else {
                            // Enforce kick only after multiple failed checks
                            if (addStrike(uuid)) {
                                resetStrikes(uuid);
                                results.put(uuid, VerificationResult.denied(
                                        MessageUtils.buildReverificationFailureMessage(config), identity));
                            } else {
                                results.put(uuid, VerificationResult.allowed(identity));
                            }
                        }
                    }
                    return results;
                });
    }
}
