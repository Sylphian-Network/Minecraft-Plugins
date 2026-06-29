package net.sylphian.minecraft.cooking.db.api;

import net.sylphian.minecraft.cooking.db.models.CookingMasteryModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for cooking mastery persistence operations.
 * All methods return {@link CompletableFuture} to ensure database
 * access never blocks the main server thread.
 */
public interface ICookingMasteryRepository {

    /**
     * Finds the mastery record for a specific player and recipe.
     *
     * @param playerUuid the player's UUID
     * @param recipeId   the recipe identifier
     * @return a CompletableFuture containing the entry if it exists
     */
    CompletableFuture<Optional<CookingMasteryModel>> findEntry(UUID playerUuid, String recipeId);

    /**
     * Retrieves all mastery records for a player.
     *
     * @param playerUuid the player's UUID
     * @return a CompletableFuture containing all mastery records for this player
     */
    CompletableFuture<List<CookingMasteryModel>> findAllForPlayer(UUID playerUuid);

    /**
     * Increments the cook count for a player and recipe by one,
     * creating a new record if none exists.
     *
     * @param playerUuid the player's UUID
     * @param recipeId   the recipe identifier
     * @return a CompletableFuture that completes when the record is saved
     */
    CompletableFuture<Void> incrementCount(UUID playerUuid, String recipeId);
}
