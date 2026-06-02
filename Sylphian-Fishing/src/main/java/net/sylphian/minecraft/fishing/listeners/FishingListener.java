package net.sylphian.minecraft.fishing.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import net.sylphian.minecraft.fishing.services.*;
import net.sylphian.minecraft.fishing.services.bait.BaitZone;
import net.sylphian.minecraft.fishing.services.mutation.FishContext;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final LootService lootService;
    private final FishMutationService mutationService;
    private final CatchEffectService catchEffectService;
    private final BiteTimerService biteTimerService;
    private final BaitZoneService baitZoneService;
    private final IFishEncyclopaediaRepository encyclopaediaRepository;
    private final JavaPlugin plugin;

    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();

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
                           BaitZoneService baitZoneService,
                           IFishEncyclopaediaRepository encyclopaediaRepository, JavaPlugin plugin) {
        this.lootService = lootService;
        this.mutationService = mutationService;
        this.catchEffectService = catchEffectService;
        this.biteTimerService = biteTimerService;
        this.baitZoneService = baitZoneService;
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
                startActionBarTask(event.getPlayer(), event.getHook());
            }
            case CAUGHT_FISH -> {
                stopActionBarTask(event.getPlayer());
                handleCatch(event);
            }
            case REEL_IN, IN_GROUND, CAUGHT_ENTITY ->
                    stopActionBarTask(event.getPlayer());
        }
    }

    /**
     * Cleans up the action bar task when a player disconnects.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopActionBarTask(event.getPlayer());
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

        BaitZone zone = baitZoneService.getZoneAt(hookLocation);
        CatchResult result = lootService.rollCatch(
                biome, weather, hookLocation.getY(), world.getTime(),
                zone != null ? zone.config() : null);
        ItemStack itemStack = result.itemStack();

        mutationService.applyMutations(event.getPlayer(), itemStack,
                new FishContext(result.rarity(), biome, event.getPlayer()));
        caughtItem.setItemStack(itemStack);
        catchEffectService.apply(event.getPlayer(), result, hookLocation);
        recordCatchAsync(event.getPlayer(), result);
    }

    /**
     * Starts a repeating task that updates the player's action bar with
     * the names of any bait zones currently containing the hook.
     * Cancels any existing task for this player first.
     *
     * @param player the fishing player
     * @param hook   the active fishing hook to track
     */
    private void startActionBarTask(Player player, org.bukkit.entity.FishHook hook) {
        stopActionBarTask(player);

        UUID uuid = player.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!hook.isValid()) {
                stopActionBarTask(player);
                return;
            }

            if (hook.getState() != org.bukkit.entity.FishHook.HookState.BOBBING) return;

            List<BaitZone> zones = baitZoneService.getZonesAt(hook.getLocation());

            if (zones.isEmpty()) {
                player.sendActionBar(Component.empty());
                return;
            }

            player.sendActionBar(buildActionBarMessage(zones));
        }, 0L, 10L);

        actionBarTasks.put(uuid, task);
    }

    /**
     * Cancels the action bar task for the given player and clears their action bar.
     *
     * @param player the player to clean up
     */
    private void stopActionBarTask(Player player) {
        BukkitTask task = actionBarTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendActionBar(Component.empty());
        }
    }

    /**
     * Builds the action bar component listing all active bait zones by display name.
     *
     * @param zones the active bait zones at the hook location
     * @return the formatted action bar component
     */
    private Component buildActionBarMessage(List<BaitZone> zones) {
        String names = zones.stream()
                .map(zone -> zone.config().displayName())
                .collect(Collectors.joining("<gray>, "));

        return MINI.deserialize("<gray>Active Baits: " + names);
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