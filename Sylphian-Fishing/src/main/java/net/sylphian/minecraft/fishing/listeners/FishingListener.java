package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutationService;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class FishingListener implements Listener {

    private final LootManager lootManager;
    private final FishMutationService mutationService;
    private final IFishEncyclopaediaRepository encyclopaediaRepository;
    private final JavaPlugin plugin;

    public FishingListener(LootManager lootManager, FishMutationService mutationService, IFishEncyclopaediaRepository encyclopaediaRepository, JavaPlugin plugin) {
        this.lootManager = lootManager;
        this.mutationService = mutationService;
        this.encyclopaediaRepository = encyclopaediaRepository;
        this.plugin = plugin;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        World world = event.getHook().getLocation().getWorld();
        Biome biome = world.getBiome(event.getHook().getLocation());
        WeatherCondition weather = WeatherCondition.from(world);

        CatchResult result = lootManager.rollCatch(biome, weather);
        ItemStack itemStack = result.itemStack();

        FishContext context = new FishContext(result.rarity(), biome, event.getPlayer());
        mutationService.applyMutations(event.getPlayer(), itemStack, context);

        caughtItem.setItemStack(itemStack);

        encyclopaediaRepository.recordCatch(
                event.getPlayer().getUniqueId(),
                result.fishId(),
                result.weight()
        ).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to record catch for " + event.getPlayer().getName() + ": " + ex.getMessage());
            return null;
        });
    }
}