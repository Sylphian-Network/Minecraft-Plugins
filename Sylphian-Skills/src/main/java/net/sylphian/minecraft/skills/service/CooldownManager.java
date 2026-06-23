package net.sylphian.minecraft.skills.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player, per-ability cooldowns for active skills.
 *
 * <p>Thread-safe. Skills read and write cooldowns from their event handlers,
 * which may run on the main thread or async. All state is cleared when a
 * player is unloaded so memory does not leak on disconnect.</p>
 */
public final class CooldownManager {

    /** uuid to (abilityId to expiry instant). */
    private final Map<UUID, Map<String, Instant>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Returns whether the player is currently on cooldown for the given ability.
     *
     * @param uuid      the player's UUID
     * @param abilityId the ability identifier (e.g. {@code "fishing:patient-angler"})
     * @return {@code true} if the cooldown has not yet expired
     */
    public boolean isOnCooldown(UUID uuid, String abilityId) {
        Map<String, Instant> player = cooldowns.get(uuid);
        if (player == null) return false;
        Instant expiry = player.get(abilityId);
        return expiry != null && Instant.now().isBefore(expiry);
    }

    /**
     * Starts a cooldown for the player on the given ability.
     *
     * @param uuid      the player's UUID
     * @param abilityId the ability identifier
     * @param duration  how long the cooldown lasts
     */
    public void setCooldown(UUID uuid, String abilityId, Duration duration) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                 .put(abilityId, Instant.now().plus(duration));
    }

    /**
     * Returns the whole seconds remaining on a cooldown, or {@code 0} if not active.
     *
     * @param uuid      the player's UUID
     * @param abilityId the ability identifier
     * @return seconds remaining, minimum 0
     */
    public long getRemainingSeconds(UUID uuid, String abilityId) {
        Map<String, Instant> player = cooldowns.get(uuid);
        if (player == null) return 0L;
        Instant expiry = player.get(abilityId);
        if (expiry == null) return 0L;
        return Math.max(0L, Duration.between(Instant.now(), expiry).getSeconds());
    }

    /**
     * Removes all cooldown state for a player. Call on player quit/unload.
     *
     * @param uuid the player's UUID
     */
    public void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
