package net.sylphian.minecraft.clans.service;

import net.sylphian.minecraft.clans.cache.TerritoryCache;
import net.sylphian.minecraft.clans.db.api.IClaimRepository;
import net.sylphian.minecraft.clans.db.models.ClaimModel;
import net.sylphian.minecraft.clans.event.ClanClaimEvent;
import net.sylphian.minecraft.clans.event.ClanUnclaimEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
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
     * @return the maximum number of chunks a single clan may own
     */
    public int getMaxClaimsPerClan() {
        return maxClaimsPerClan;
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
                                .callEvent(new ClanClaimEvent.Post(clanId, world, chunkX, chunkZ)));
            });
        });
    }

    /**
     * Claims many chunks at once on behalf of a clan.
     *
     * <p>Chunks already claimed (by any clan) are skipped using the cache, and the
     * per-clan limit is enforced once for the whole batch: if the candidates would
     * exceed the limit, only as many as fit are claimed. The survivors are inserted
     * in a single transaction, then the cache is updated and one
     * {@link ClanClaimEvent.Post} is fired per claimed chunk on the main thread.</p>
     *
     * @param clanId the clan claiming the chunks
     * @param world  the world name
     * @param chunks the candidate chunks as {@code {chunkX, chunkZ}} pairs
     * @return a future of the chunks actually claimed (a subset of the input; empty if none)
     * @throws IllegalStateException if the clan is already at its claim limit
     */
    public CompletableFuture<List<int[]>> claimChunks(UUID clanId, String world, List<int[]> chunks) {
        // Pre fires per chunk on the main thread (the calling command's context); cancelled chunks are dropped.
        List<int[]> allowed = new ArrayList<>();
        for (int[] c : chunks) {
            ClanClaimEvent.Pre pre = new ClanClaimEvent.Pre(clanId, world, c[0], c[1]);
            plugin.getServer().getPluginManager().callEvent(pre);
            if (!pre.isCancelled()) allowed.add(c);
        }
        if (allowed.isEmpty()) {
            return CompletableFuture.completedFuture(List.<int[]>of());
        }

        return claimRepository.findClaimsByClan(clanId).thenCompose(existing -> {
            int remaining = maxClaimsPerClan - existing.size();
            if (remaining <= 0) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Your clan has reached the maximum claim limit of " + maxClaimsPerClan + "."));
            }

            List<int[]> toClaim = allowed.stream()
                    .filter(c -> !territoryCache.isClaimed(world, c[0], c[1]))
                    .limit(remaining)
                    .toList();

            if (toClaim.isEmpty()) {
                return CompletableFuture.completedFuture(List.<int[]>of());
            }

            Instant now = Instant.now();
            List<ClaimModel> models = toClaim.stream()
                    .map(c -> new ClaimModel(world, c[0], c[1], clanId, now))
                    .toList();

            return claimRepository.insertClaims(models).thenApply(v -> {
                toClaim.forEach(c -> territoryCache.put(world, c[0], c[1], clanId));
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        toClaim.forEach(c -> plugin.getServer().getPluginManager()
                                .callEvent(new ClanClaimEvent.Post(clanId, world, c[0], c[1]))));
                return toClaim;
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

        ClanUnclaimEvent.Pre pre = new ClanUnclaimEvent.Pre(clanId, world, chunkX, chunkZ);
        plugin.getServer().getPluginManager().callEvent(pre);
        if (pre.isCancelled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unclaim was cancelled."));
        }

        return claimRepository.deleteClaim(world, chunkX, chunkZ).thenRun(() -> {
            territoryCache.remove(world, chunkX, chunkZ);
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getServer().getPluginManager()
                            .callEvent(new ClanUnclaimEvent.Post(clanId, world, chunkX, chunkZ)));
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
