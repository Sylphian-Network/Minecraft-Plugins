package net.sylphian.minecraft.fishing.mutation.impl;

import io.papermc.paper.registry.RegistryKey;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static io.papermc.paper.registry.RegistryAccess.registryAccess;
import static net.sylphian.minecraft.fishing.SylphianFishingBootstrap.SUPER_FISH_KEY;

public class SuperFishMutation implements FishMutation {

    @Override
    public boolean shouldApply(Player player, FishContext context) {
        return true;
    }

    @Override
    public void apply(ItemStack item, Player player, FishContext context) {
        Enchantment superFish = registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(SUPER_FISH_KEY);

        if (superFish != null) {
            item.addUnsafeEnchantment(superFish, 1);
        }
    }
}
