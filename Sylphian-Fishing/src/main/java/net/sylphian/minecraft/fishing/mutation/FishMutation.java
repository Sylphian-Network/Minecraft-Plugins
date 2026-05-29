package net.sylphian.minecraft.fishing.mutation;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface FishMutation {
    boolean shouldApply(Player player, FishContext context);
    void apply(ItemStack item, Player player, FishContext context);
}
