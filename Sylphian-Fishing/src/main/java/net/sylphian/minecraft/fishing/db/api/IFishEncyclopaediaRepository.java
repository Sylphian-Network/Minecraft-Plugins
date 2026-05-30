package net.sylphian.minecraft.fishing.db.api;

import net.sylphian.minecraft.fishing.db.models.FishEncyclopaediaModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for fish encyclopaedia persistence operations.
 * All methods return CompletableFuture to ensure database
 * access never blocks the main server thread.
 */
public interface IFishEncyclopaediaRepository {
    /**
     * Finds a specific fish entry in the player's encyclopaedia.
     *
     * @param uuid   the player's UUID
     * @param fishId the ID of the fish to find
     * @return a CompletableFuture containing an Optional with the entry if found
     */
    CompletableFuture<Optional<FishEncyclopaediaModel>> findEntry(UUID uuid, String fishId);

    /**
     * Retrieves all fish entries for a specific player.
     *
     * @param uuid the player's UUID
     * @return a CompletableFuture containing a list of all caught fish entries
     */
    CompletableFuture<List<FishEncyclopaediaModel>> findAllForPlayer(UUID uuid);

    /**
     * Records a fish catch in the database.
     * If the fish has never been caught by the player, a new entry is created.
     * Otherwise, statistics (count, biggest weight, last caught time) are updated.
     *
     * @param uuid   the player's UUID
     * @param fishId the ID of the fish caught
     * @param weight the weight of the caught fish
     * @return a CompletableFuture that completes when the record is saved
     */
    CompletableFuture<Void> recordCatch(UUID uuid, String fishId, double weight);
}