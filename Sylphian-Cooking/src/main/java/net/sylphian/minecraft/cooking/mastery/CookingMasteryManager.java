package net.sylphian.minecraft.cooking.mastery;

import net.sylphian.minecraft.cooking.db.api.ICookingMasteryRepository;
import net.sylphian.minecraft.cooking.db.models.CookingMasteryModel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory cache of per-player cooking mastery counts, backed by {@link ICookingMasteryRepository}.
 *
 * <p>On player join the cache is seeded from the database asynchronously.
 * Increment writes update the cache synchronously and persist to the database
 * asynchronously. On quit the player's entry is evicted.</p>
 *
 * <p>This class implements {@link MasteryAccessor} for use by {@link
 * net.sylphian.minecraft.cooking.station.CookingStationService}.</p>
 */
public final class CookingMasteryManager implements MasteryAccessor, Listener {

    private final ICookingMasteryRepository repository;
    private final Logger logger;

    /** Cook counts keyed by player UUID, then recipe ID. */
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Integer>> cache =
            new ConcurrentHashMap<>();

    /**
     * @param repository the async mastery repository
     * @param logger     plugin logger for error reporting
     */
    public CookingMasteryManager(ICookingMasteryRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        repository.findAllForPlayer(uuid).thenAccept(entries -> seed(uuid, entries))
                .exceptionally(ex -> {
                    logger.warning("Failed to load cooking mastery for " + uuid + ": " + ex.getMessage());
                    return null;
                });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public int getCount(UUID playerUuid, String recipeId) {
        ConcurrentHashMap<String, Integer> counts = cache.get(playerUuid);
        if (counts == null) return 0;
        return counts.getOrDefault(recipeId, 0);
    }

    @Override
    public void increment(UUID playerUuid, String recipeId) {
        cache.computeIfAbsent(playerUuid, _ -> new ConcurrentHashMap<>())
                .merge(recipeId, 1, Integer::sum);

        repository.incrementCount(playerUuid, recipeId)
                .exceptionally(ex -> {
                    logger.warning("Failed to persist mastery increment for " + playerUuid
                            + " recipe " + recipeId + ": " + ex.getMessage());
                    return null;
                });
    }

    /**
     * Seeds mastery counts for all players currently online.
     * Call once from {@code onEnable()} to cover players who were online before the plugin loaded.
     *
     * @param onlinePlayers the collection of currently online player UUIDs
     */
    public void seedOnlinePlayers(Collection<UUID> onlinePlayers) {
        for (UUID uuid : onlinePlayers) {
            repository.findAllForPlayer(uuid).thenAccept(entries -> seed(uuid, entries))
                    .exceptionally(ex -> {
                        logger.warning("Failed to load cooking mastery for " + uuid + ": " + ex.getMessage());
                        return null;
                    });
        }
    }

    /**
     * Merges DB counts into the cache, taking the higher value per recipe so that
     * any increment that fired before the async load completed is not lost.
     */
    private void seed(UUID uuid, List<CookingMasteryModel> entries) {
        ConcurrentHashMap<String, Integer> counts = cache.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>());
        for (CookingMasteryModel entry : entries) {
            counts.merge(entry.recipeId(), entry.cookCount(), Integer::max);
        }
    }
}
