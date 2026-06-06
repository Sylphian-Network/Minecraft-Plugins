package net.sylphian.minecraft.cooking.recipe;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a single ingredient requirement in a {@link CookingRecipe}.
 * Implementations define how an ItemStack is matched against the requirement.
 */
public interface IngredientSpec {

    /**
     * Returns true if the given ItemStack satisfies this ingredient requirement.
     *
     * @param stack the item to test; may be null or air
     * @return true if the item matches
     */
    boolean matches(ItemStack stack);

    /**
     * A human-readable identifier for this ingredient, used in logging.
     * For vanilla materials this is the material name; for namespaced items it is the full ID.
     *
     * @return the display identifier
     */
    String displayId();
}
