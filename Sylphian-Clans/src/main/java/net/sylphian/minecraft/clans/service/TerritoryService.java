package net.sylphian.minecraft.clans.service;

import net.sylphian.minecraft.clans.cache.TerritoryCache;
import net.sylphian.minecraft.clans.db.api.IClaimRepository;
import net.sylphian.minecraft.clans.db.models.ClaimModel;
import net.sylphian.minecraft.clans.event.TerritoryClaimEvent;
import net.sylphian.minecraft.clans.event.TerritoryUnclaimEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Business logic for claiming and unclaiming chunks on behalf of clans.
 *
 * <p>Writes are always DB-first: {@link TerritoryCache} is updated only after the
 * DB operation completes successfully. Events are fired on the main thread via the
 * Bukkit scheduler.</p>
 */
public class TerritoryService {

    private final IClaimRepository claimRepository;
    private final TerritoryCache territoryCache;
    private final JavaPlugin plugin;
    private int maxClaimsPerClan;

    /**
     * @param claimRepository  persistence layer for claims
     * @param territoryCache   in-memory chunk-ownership cache
     * @param plugin           the owning plugin, used for scheduler hops
     * @param maxClaimsPerClan the maximum number of chunks a single clan may own
     */
    public TerritoryService(IClaimRepository claimRepository, TerritoryCache territoryCache,
                            JavaPlugin plugin, int maxClaimsPerClan) {
        this.claimRepository = claimRepository;
        this.territoryCache = territoryCache;
        this.plugin = plugin;
        this.maxClaimsPerClan = maxClaimsPerClan;
    }

    /**
     * Updates the maximum claims-per-clan limit. Called after a config reload.
     *
     * @param max the new limit
     */
    public void setMaxClaimsPerClan(int max) {
        this.maxClaimsPerClan = max;
    }

    /**
     * Claims a chunk on behalf of a clan.
     *
     * <p>Fails silently (returned future completes exceptionally) if the chunk is
     * already claimed or the clan has reached its claim limit.</p>
     *
     * @param clanId the clan claiming the chunk
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a future that completes when the claim is persisted and cached
     * @throws IllegalStateException if the chunk is already claimed or the clan is at its limit
     */
    public CompletableFuture<Void> claimChunk(UUID clanId, String world, int chunkX, int chunkZ) {
        if (territoryCache.isClaimed(world, chunkX, chunkZ)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Chunk " + world + ":" + chunkX + ":" + chunkZ + " is already claimed."));
        }

        return claimRepository.findClaimsByClan(clanId).thenCompose(existing -> {
            if (existing.size() >= maxClaimsPerClan) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Clan has reached the maximum claim limit of " + maxClaimsPerClan + "."));
            }

            ClaimModel model = new ClaimModel(world, chunkX, chunkZ, clanId, Instant.now());
            return claimRepository.insertClaim(model).thenRun(() -> {
                territoryCache.put(world, chunkX, chunkZ, clanId);
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.getServer().getPluginManager()
                                .callEvent(new TerritoryClaimEvent(clanId, world, chunkX, chunkZ)));
            });
        });
    }

    /**
     * Unclaims a chunk owned by the given clan.
     *
     * @param clanId the clan that owns the chunk
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a future that completes when the claim is removed from the DB and cache
     * @throws IllegalStateException if the chunk is not owned by the given clan
     */
    public CompletableFuture<Void> unclaimChunk(UUID clanId, String world, int chunkX, int chunkZ) {
        Optional<UUID> owner = territoryCache.get(world, chunkX, chunkZ);
        if (owner.isEmpty() || !owner.get().equals(clanId)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Clan does not own chunk " + world + ":" + chunkX + ":" + chunkZ + "."));
        }

        return claimRepository.deleteClaim(world, chunkX, chunkZ).thenRun(() -> {
            territoryCache.remove(world, chunkX, chunkZ);
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getServer().getPluginManager()
                            .callEvent(new TerritoryUnclaimEvent(clanId, world, chunkX, chunkZ)));
        });
    }

    /**
     * Removes all claims for a clan. Used during clan disbandment.
     *
     * @param clanId the clan whose claims to remove
     * @return a future that completes when all claims are removed
     */
    public CompletableFuture<Void> unclaimAll(UUID clanId) {
        return claimRepository.deleteAllClaimsForClan(clanId)
                .thenRun(() -> territoryCache.removeAllForClan(clanId));
    }

    /**
     * Returns the owning clan for a chunk. Reads from the cache.
     *
     * @param world  the world name
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the owning clan's UUID, or empty if the chunk is unclaimed
     */
    public Optional<UUID> getClaimingClan(String world, int chunkX, int chunkZ) {
        return territoryCache.get(world, chunkX, chunkZ);
    }

    /**
     * Seeds the {@link TerritoryCache} from the database. Called once on startup.
     *
     * @return a future that completes when the cache is populated
     */
    public CompletableFuture<Void> seedCache() {
        return claimRepository.findAllClaims().thenAccept(claims ->
                claims.forEach(c -> territoryCache.put(c.world(), c.chunkX(), c.chunkZ(), c.clanId())));
    }

    /**
     * @param clanId the clan to query
     * @return a future of all claims currently owned by the clan
     */
    public CompletableFuture<List<ClaimModel>> getClaimsForClan(UUID clanId) {
        return claimRepository.findClaimsByClan(clanId);
    }
}
