package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.db.api.IFishEncyclopaediaRepository;
import net.sylphian.minecraft.fishing.fish.CatchResult;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutationService;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class FishingListener implements Listener {

    private final LootManager lootManager;
    private final FishMutationService mutationService;
    private final IFishEncyclopaediaRepository encyclopaediaRepository;

    public FishingListener(LootManager lootManager, FishMutationService mutationService, IFishEncyclopaediaRepository encyclopaediaRepository) {
        this.lootManager = lootManager;
        this.mutationService = mutationService;
        this.encyclopaediaRepository = encyclopaediaRepository;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        Biome biome = event.getHook().getLocation().getWorld()
                .getBiome(event.getHook().getLocation());

        CatchResult result = lootManager.rollCatch(biome);
        ItemStack itemStack = result.itemStack();

        FishContext context = new FishContext(result.rarity(), biome, event.getPlayer());
        mutationService.applyMutations(event.getPlayer(), itemStack, context);

        caughtItem.setItemStack(itemStack);

        encyclopaediaRepository.recordCatch(
                event.getPlayer().getUniqueId(),
                result.fishId(),
                result.weight()
        );
    }
}