package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import net.sylphian.minecraft.fishing.services.*;
import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.services.bait.BaitZone;

import java.util.List;
import net.sylphian.minecraft.fishing.services.mutation.FishContext;
import net.sylphian.minecraft.fishing.sidebar.FishingContributor;
import net.sylphian.minecraft.scoreboard.services.SidebarService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central listener for all player fishing events.
 *
 * <p>Delegates each fishing state to the appropriate service:</p>
 * <ul>
 *   <li>{@link PlayerFishEvent.State#FISHING} — applies a custom bite timer and starts
 *       the action bar task showing any active bait zones at the hook location</li>
 *   <li>{@link PlayerFishEvent.State#CAUGHT_FISH} — rolls loot (applying any active bait zone
 *       bonuses), applies mutations, triggers effects, and records the catch asynchronously</li>
 *   <li>All terminal states — clears the action bar and cancels the zone display task</li>
 * </ul>
 */
public class FishingListener implements Listener {

    private final LootService lootService;
    private final FishMutationService mutationService;
    private final CatchEffectService catchEffectService;
    private final BiteTimerService biteTimerService;
    private final BaitZoneService baitZoneService;
    private final FishingContributor baitContributor;
    private final IFishEncyclopaediaRepository encyclopaediaRepository;
    private final JavaPlugin plugin;

    /**
     * Constructs a new FishingListener.
     *
     * @param lootService             the service for rolling catches
     * @param mutationService         the service for applying mutations
     * @param catchEffectService      the service for applying rarity catch effects
     * @param biteTimerService        the service for applying custom bite delays
     * @param baitZoneService         the service for querying active bait zones
     * @param encyclopaediaRepository the repository for recording catches
     * @param plugin                  the plugin instance for logging and scheduling
     */
    public FishingListener(LootService lootService, FishMutationService mutationService,
                           CatchEffectService catchEffectService, BiteTimerService biteTimerService,
                           BaitZoneService baitZoneService, FishingContributor baitContributor,
                           IFishEncyclopaediaRepository encyclopaediaRepository, JavaPlugin plugin) {
        this.lootService = lootService;
        this.mutationService = mutationService;
        this.catchEffectService = catchEffectService;
        this.biteTimerService = biteTimerService;
        this.baitZoneService = baitZoneService;
        this.baitContributor = baitContributor;
        this.encyclopaediaRepository = encyclopaediaRepository;
        this.plugin = plugin;
    }

    /**
     * Routes fishing state transitions to the appropriate handler.
     *
     * @param event the fishing event
     */
    @EventHandler
    public void onFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case FISHING -> {
                biteTimerService.applyBiteTimer(event.getHook(), event.getPlayer());
                baitContributor.trackHook(event.getPlayer().getUniqueId(), event.getHook());
                SidebarService.refresh(event.getPlayer());
            }
            case CAUGHT_FISH -> {
                baitContributor.clearHook(event.getPlayer().getUniqueId());
                SidebarService.refresh(event.getPlayer());
                handleCatch(event);
            }
            case REEL_IN, IN_GROUND, CAUGHT_ENTITY -> {
                baitContributor.clearHook(event.getPlayer().getUniqueId());
                SidebarService.refresh(event.getPlayer());
            }
        }
    }

    /**
     * Cleans up the action bar task when a player disconnects.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        baitContributor.clearHook(event.getPlayer().getUniqueId());
    }

    /**
     * Handles a successful catch — rolls loot, applies mutations, triggers
     * rarity effects, and records the catch to the database.
     *
     * @param event the fishing event in the CAUGHT_FISH state
     */
    private void handleCatch(PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        event.setExpToDrop(0);

        Location hookLocation = event.getHook().getLocation();
        World world = hookLocation.getWorld();
        if (world == null) return;

        Biome biome = world.getBiome(hookLocation);
        WeatherCondition weather = WeatherCondition.from(world);

        List<BaitConfig> baitBonuses = baitZoneService.getZonesAt(hookLocation).stream()
                .map(BaitZone::config)
                .toList();
        CatchResult result = lootService.rollCatch(
                biome, weather, hookLocation.getY(), world.getTime(), baitBonuses);
        ItemStack itemStack = result.itemStack();

        mutationService.applyMutations(event.getPlayer(), itemStack,
                new FishContext(result.rarity(), biome, event.getPlayer()));
        caughtItem.setItemStack(itemStack);
        catchEffectService.apply(event.getPlayer(), result, hookLocation);
        recordCatchAsync(event.getPlayer(), result);
    }

    /**
     * Records the catch asynchronously via the encyclopaedia repository.
     *
     * @param player the player who caught the fish
     * @param result the catch result to record
     */
    private void recordCatchAsync(Player player, CatchResult result) {
        encyclopaediaRepository.recordCatch(player.getUniqueId(), result.fishId(), result.weight())
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to record catch for "
                            + player.getName() + ": " + ex.getMessage());
                    return null;
                });
    }
}