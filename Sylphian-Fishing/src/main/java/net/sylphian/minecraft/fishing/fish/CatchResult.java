package net.sylphian.minecraft.fishing.fish;

import org.bukkit.inventory.ItemStack;

public record CatchResult(String fishId, Rarity rarity, double weight, ItemStack itemStack) {}
