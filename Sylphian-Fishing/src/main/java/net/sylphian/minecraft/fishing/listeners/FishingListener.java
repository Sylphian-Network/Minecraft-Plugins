package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.effects.CatchEffectService;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutationService;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener for player fishing events.
 * Intercepts successful catches to determine the fish type, apply mutations,
 * and record the catch in the player's encyclopaedia.
 */
public class FishingListener implements Listener {

    private final LootManager lootManager;
    private final FishMutationService mutationService;
    private final CatchEffectService catchEffectService;
    private final IFishEncyclopaediaRepository encyclopaediaRepository;
    private final JavaPlugin plugin;

    /**
     * Constructs a new FishingListener.
     *
     * @param lootManager             the manager for rolling catches
     * @param mutationService         the service for applying mutations
     * @param encyclopaediaRepository the repository for recording catches
     * @param plugin                  the plugin instance
     */
    public FishingListener(LootManager lootManager, FishMutationService mutationService, CatchEffectService catchEffectService, IFishEncyclopaediaRepository encyclopaediaRepository, JavaPlugin plugin) {
        this.lootManager = lootManager;
        this.mutationService = mutationService;
        this.catchEffectService = catchEffectService;
        this.encyclopaediaRepository = encyclopaediaRepository;
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerFishEvent.
     * Resolves the hook's biome, weather, Y coordinate, and world time,
     * then rolls for a fish, applies mutations, updates the caught item,
     * triggers rarity catch effects, and records the catch asynchronously.
     *
     * @param event the fishing event
     */
    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        event.setExpToDrop(0);

        Location hookLocation = event.getHook().getLocation();
        World world = hookLocation.getWorld();
        if (world == null) return;

        Biome biome = world.getBiome(hookLocation);
        WeatherCondition weather = WeatherCondition.from(world);

        CatchResult result = lootManager.rollCatch(biome, weather, hookLocation.getY(), world.getTime());
        ItemStack itemStack = result.itemStack();

        mutationService.applyMutations(event.getPlayer(), itemStack, new FishContext(result.rarity(), biome, event.getPlayer()));
        caughtItem.setItemStack(itemStack);
        catchEffectService.apply(event.getPlayer(), result, hookLocation);
        recordCatchAsync(event.getPlayer(), result);
    }

    private void recordCatchAsync(Player player, CatchResult result) {
        encyclopaediaRepository.recordCatch(player.getUniqueId(), result.fishId(), result.weight())
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to record catch for " + player.getName() + ": " + ex.getMessage());
                    return null;
                });
    }
}