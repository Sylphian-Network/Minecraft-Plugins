package net.sylphian.minecraft.cooking.mastery;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides read and write access to per-player cooking mastery counts.
 * Implementations may be backed by a database or an in-memory stub.
 */
public interface MasteryAccessor {

    /**
     * Returns the number of times the player has successfully cooked the given recipe.
     *
     * @param playerUuid the player's UUID
     * @param recipeId   the recipe identifier
     * @return the cook count, or 0 if no record exists
     */
    int getCount(UUID playerUuid, String recipeId);

    /**
     * Increments the cook count for the player and recipe by one and returns the new count.
     *
     * @param playerUuid the player's UUID
     * @param recipeId   the recipe identifier
     * @return a future containing the authoritative cook count after the increment,
     *         or a non-positive value if the count could not be determined
     */
    CompletableFuture<Integer> increment(UUID playerUuid, String recipeId);
}
