package net.sylphian.minecraft.skills.db.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence contract for player skill data.
 */
public interface ISkillRepository {

    /**
     * Loads all skill XP values for a player.
     *
     * @param uuid the player's UUID
     * @return a future of a map from skill ID to total XP
     */
    CompletableFuture<Map<String, Long>> loadAll(UUID uuid);

    /**
     * Loads the XP for a single skill. Returns 0 if no row exists yet.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @return a future of the stored XP, or {@code 0} if not found
     */
    CompletableFuture<Long> loadOne(UUID uuid, String skillId);

    /**
     * Persists a player's XP for a single skill.
     * Uses an upsert that keeps the higher of the stored and incoming value,
     * so out-of-order async writes can never overwrite a newer result.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill identifier
     * @param xp      the new total XP
     * @return a future that completes when the write finishes
     */
    CompletableFuture<Void> upsertXP(UUID uuid, String skillId, long xp);
}
