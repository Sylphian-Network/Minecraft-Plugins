package net.sylphian.minecraft.cooking.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Represents a single cooking recipe.
 *
 * <p>A recipe specifies up to five ingredients (matched as an unordered multiset),
 * a cook time in ticks, and the ItemStack produced on completion.</p>
 *
 * @param id                  unique recipe identifier from {@code recipes.yml}
 * @param ingredients         ordered list of ingredient specs; 1–5 entries
 * @param cookTime            time in ticks required to complete this recipe
 * @param output              the ItemStack produced when cooking completes
 * @param qualityBonusEnabled whether the mastery quality bonus applies to this recipe
 * @param customModelData     custom model data float to apply to the output icon, or -1 if unset
 */
public record CookingRecipe(String id, List<IngredientSpec> ingredients, int cookTime, ItemStack output,
                            boolean qualityBonusEnabled, float customModelData) {

    /**
     * Returns true if the given ingredient slots satisfy this recipe.
     *
     * <p>Matching is a multiset comparison: each non-null/non-air slot item must match
     * exactly one unmatched ingredient spec, and every spec must be matched by exactly
     * one slot. Slots beyond the recipe's ingredient count must be empty.</p>
     *
     * @param slots the contents of the five ingredient slots (length must be 5)
     * @return true if the slots contain exactly the right items for this recipe
     */
    public boolean matches(ItemStack[] slots) {
        // Collect non-empty slot items
        List<ItemStack> provided = java.util.Arrays.stream(slots)
                .filter(s -> s != null && !s.getType().isAir())
                .toList();

        if (provided.size() != ingredients.size()) return false;

        // Each spec must be satisfied by exactly one provided item (greedy left-to-right)
        boolean[] used = new boolean[provided.size()];
        for (IngredientSpec spec : ingredients) {
            boolean found = false;
            for (int i = 0; i < provided.size(); i++) {
                if (!used[i] && spec.matches(provided.get(i))) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
