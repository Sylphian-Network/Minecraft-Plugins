package net.sylphian.minecraft.fishing.services.mutation;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Interface representing a mutation that can be applied to a caught fish.
 * Mutations can modify the fish's ItemStack, add metadata, or apply effects to the player.
 */
public interface FishMutation {
    /**
     * Determines if the mutation should be applied based on the player and context.
     *
     * @param player  the player who caught the fish
     * @param context the catch context
     * @return true if the mutation should apply, false otherwise
     */
    boolean shouldApply(Player player, FishContext context);

    /**
     * Applies the mutation to the caught fish item.
     *
     * @param item    the fish item stack
     * @param player  the player who caught the fish
     * @param context the catch context
     */
    void apply(ItemStack item, Player player, FishContext context);
}
