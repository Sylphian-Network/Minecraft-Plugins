package net.sylphian.minecraft.cooking.db.models;

import java.util.UUID;

/**
 * Data model representing a player's mastery record for a single recipe.
 *
 * @param playerUuid the player's unique identifier
 * @param recipeId   the recipe identifier
 * @param cookCount  the number of times the player has cooked this recipe
 */
public record CookingMasteryModel(
        UUID playerUuid,
        String recipeId,
        int cookCount
) {}
