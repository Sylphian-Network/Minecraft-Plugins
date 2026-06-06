package net.sylphian.minecraft.cooking.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Matches a vanilla Minecraft item by {@link Material} type.
 * Any ItemStack whose material equals the configured value is accepted,
 * regardless of display name, lore, or other meta.
 */
public record MaterialIngredientSpec(Material material) implements IngredientSpec {

    @Override
    public boolean matches(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && stack.getType() == material;
    }

    @Override
    public String displayId() {
        return material.name();
    }
}
