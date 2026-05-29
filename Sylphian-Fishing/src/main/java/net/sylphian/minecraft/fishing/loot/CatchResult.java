package net.sylphian.minecraft.fishing.loot;

import net.sylphian.minecraft.fishing.fish.Rarity;
import org.bukkit.inventory.ItemStack;

public record CatchResult(String fishId, Rarity rarity, double weight, ItemStack itemStack) {}
