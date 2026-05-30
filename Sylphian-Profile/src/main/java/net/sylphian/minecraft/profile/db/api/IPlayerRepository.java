package net.sylphian.minecraft.profile.db.api;

import net.sylphian.minecraft.profile.db.models.PlayerModel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for player data persistence operations.
 * All methods return CompletableFuture to ensure database operations
 * do not block the main Minecraft server thread.
 */
public interface IPlayerRepository {
    /**
     * Finds a player by their Mojang UUID.
     *
     * @param uuid the player's UUID
     * @return a future containing an optional player model
     */
    CompletableFuture<Optional<PlayerModel>> findByUuid(UUID uuid);

    /**
     * Finds a player by their linked XenForo user ID.
     *
     * @param xfUserId the XenForo user ID
     * @return a future containing an optional player model
     */
    CompletableFuture<Optional<PlayerModel>> findByXfUserId(Integer xfUserId);

    /**
     * Inserts a new player record into the database.
     *
     * @param player the player model to insert
     * @return a future that completes when the insertion is finished
     */
    CompletableFuture<Void> insert(PlayerModel player);

    /**
     * Updates an existing player record in the database.
     *
     * @param player the player model with updated data
     * @return a future that completes when the update is finished
     */
    CompletableFuture<Void> update(PlayerModel player);
}
