package net.sylphian.minecraft.fishing.mutation.impl;

import io.papermc.paper.registry.RegistryKey;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static io.papermc.paper.registry.RegistryAccess.registryAccess;
import static net.sylphian.minecraft.fishing.SylphianFishingBootstrap.SUPER_FISH_KEY;

/**
 * A specific mutation that applies the Super Fish enchantment to a caught fish.
 * Used to indicate a "special" or "upgraded" version of a fish.
 */
public class SuperFishMutation implements FishMutation {

    /**
     * Always returns true as Super Fish can apply to any catch context.
     *
     * @param player  the player who caught the fish
     * @param context the catch context
     * @return true
     */
    @Override
    public boolean shouldApply(Player player, FishContext context) {
        return true;
    }

    /**
     * Applies the Super Fish enchantment to the item stack.
     * Uses the Paper registry to retrieve the enchantment key.
     *
     * @param item    the fish item stack
     * @param player  the player who caught the fish
     * @param context the catch context
     */
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
