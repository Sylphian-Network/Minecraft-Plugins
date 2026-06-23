package net.sylphian.minecraft.clans.api;

import net.sylphian.minecraft.clans.model.Clan;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The public, cross-plugin contract for Sylphian-Clans, obtained via {@link ClanProvider}.
 *
 * <p>Exposes read-only clan lookups. Mutation (creating clans, managing members, etc.)
 * is handled internally by Sylphian-Clans and is not part of this contract.</p>
 */
public interface ClanAPI {

    /**
     * Returns the clan the given player currently belongs to.
     *
     * @param playerUuid the player's UUID
     * @return a future of the player's clan, or empty if they are not in one
     */
    CompletableFuture<Optional<Clan>> getClanByPlayer(UUID playerUuid);

    /**
     * Returns the player's clan from the in-memory cache without hitting the database.
     * Returns empty if the player is not cached (not yet loaded or not in a clan).
     * Suitable for per-tick or per-render paths where blocking is not acceptable.
     *
     * @param playerUuid the player's UUID
     * @return the cached clan snapshot, or empty
     */
    Optional<Clan> getClanByPlayerCached(UUID playerUuid);

    /**
     * Returns a clan by its unique identifier.
     *
     * @param clanId the clan's UUID
     * @return a future of the clan, or empty if no clan with that ID exists
     */
    CompletableFuture<Optional<Clan>> getClanById(UUID clanId);

    /**
     * Returns a clan by its display name (case-insensitive).
     *
     * @param name the clan's display name
     * @return a future of the clan, or empty if no clan with that name exists
     */
    CompletableFuture<Optional<Clan>> getClanByName(String name);
}
