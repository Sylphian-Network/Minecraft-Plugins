package net.sylphian.minecraft.fishing.fish;

import org.bukkit.inventory.ItemStack;

/**
 * Represents the result of a fishing catch attempt.
 * Carries the fish ID, rolled weight, rarity, and built ItemStack
 * so that all downstream systems (encyclopaedia, loot display,
 * mutation) operate on the same rolled values.
 *
 * @param fishId    the unique ID of the caught fish
 * @param rarity    the rarity of the caught fish
 * @param weight    the rolled weight of the fish in kilograms
 * @param itemStack the physical item stack representing the fish
 */
public record CatchResult(String fishId, Rarity rarity, double weight, ItemStack itemStack) {}
