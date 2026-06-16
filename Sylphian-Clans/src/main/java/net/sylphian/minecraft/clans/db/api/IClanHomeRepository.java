package net.sylphian.minecraft.clans.db.api;

import net.sylphian.minecraft.clans.db.models.ClanHomeModel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async persistence contract for clan home locations.
 *
 * <p>All methods return {@link CompletableFuture} and execute on the shared
 * database executor. Callers must never block the main thread on these futures.</p>
 */
public interface IClanHomeRepository {

    /**
     * Persists or replaces the home location for a clan.
     *
     * @param model the home location to store
     * @return a future that completes when the row is written
     */
    CompletableFuture<Void> setHome(ClanHomeModel model);

    /**
     * @param clanId the clan's UUID
     * @return a future of the home location, or empty if no home has been set
     */
    CompletableFuture<Optional<ClanHomeModel>> getHome(UUID clanId);

    /**
     * Removes the home for a clan. No-op if no home is set.
     *
     * @param clanId the clan's UUID
     * @return a future that completes when the row is deleted
     */
    CompletableFuture<Void> deleteHome(UUID clanId);
}
