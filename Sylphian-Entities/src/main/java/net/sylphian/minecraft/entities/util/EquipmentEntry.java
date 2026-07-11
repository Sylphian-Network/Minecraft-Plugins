package net.sylphian.minecraft.entities.util;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An equipped item and its optional drop chance.
 *
 * @param item       the item to equip
 * @param dropChance the chance (0.0-1.0) the item drops on death, or null to
 *                   leave the vanilla default for the slot
 */
public record EquipmentEntry(ItemStack item, @Nullable Float dropChance) {}
