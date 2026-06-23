package net.sylphian.minecraft.skills.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which timed buff abilities are currently active per player.
 *
 * <p>This class is intentionally minimal: it records active state only.
 * Skills are responsible for scheduling their own expiry tasks (via their
 * owning plugin's scheduler) and calling {@link #removeBuff} when they expire.
 * This keeps scheduling logic in the skill, where the owning plugin is known.</p>
 *
 * <p>Thread-safe. All state is cleared on player unload.</p>
 */
public final class ActiveBuffTracker {

    /** uuid to the set of buff IDs currently active for that player. */
    private final Map<UUID, Set<String>> activeBuffs = new ConcurrentHashMap<>();

    /**
     * Returns whether the player currently has the given buff active.
     *
     * @param uuid   the player's UUID
     * @param buffId the buff identifier (e.g. {@code "fishing:fishers-frenzy"})
     * @return {@code true} if the buff is active
     */
    public boolean hasBuff(UUID uuid, String buffId) {
        Set<String> buffs = activeBuffs.get(uuid);
        return buffs != null && buffs.contains(buffId);
    }

    /**
     * Marks a buff as active for a player. The caller is responsible for
     * scheduling removal via {@link #removeBuff} when the buff expires.
     *
     * @param uuid   the player's UUID
     * @param buffId the buff identifier
     */
    public void addBuff(UUID uuid, String buffId) {
        activeBuffs.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                   .add(buffId);
    }

    /**
     * Removes a buff from a player. Call this when the buff expires or is cancelled.
     *
     * @param uuid   the player's UUID
     * @param buffId the buff identifier
     */
    public void removeBuff(UUID uuid, String buffId) {
        Set<String> buffs = activeBuffs.get(uuid);
        if (buffs == null) return;
        buffs.remove(buffId);
        if (buffs.isEmpty()) activeBuffs.remove(uuid);
    }

    /**
     * Removes all active buffs for a player. Call on player quit/unload.
     *
     * @param uuid the player's UUID
     */
    public void clearPlayer(UUID uuid) {
        activeBuffs.remove(uuid);
    }
}
