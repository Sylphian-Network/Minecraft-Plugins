package net.sylphian.minecraft.cooking.item;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.core.item.ItemProvider;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exposes the output items of all cooking recipes to Sylphian-Core's ItemRegistry.
 *
 * <p>Items are addressable as {@code sylphian-cooking:<recipe-id>}, e.g.
 * {@code "sylphian-cooking:fish_and_chips"}.</p>
 *
 * <p>This allows other plugins to reference cooked outputs as ingredients
 * in their own systems (e.g. as crate rewards or further recipe inputs).</p>
 */
public class CookingItemProvider implements ItemProvider {

    private List<CookingRecipe> recipes;

    /**
     * Constructs a new CookingItemProvider.
     *
     * @param recipes the initial list of loaded cooking recipes
     */
    public CookingItemProvider(List<CookingRecipe> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    @Override
    public String namespace() {
        return "sylphian-cooking";
    }

    @Override
    public Optional<ItemStack> provide(String itemId) {
        return recipes.stream()
                .filter(r -> r.id().equals(itemId))
                .map(r -> r.output().clone())
                .findFirst();
    }

    @Override
    public Set<String> itemIds() {
        return recipes.stream()
                .map(CookingRecipe::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Updates the recipe list used by this provider after a config reload.
     *
     * @param newRecipes the reloaded recipe list
     */
    public void reload(List<CookingRecipe> newRecipes) {
        this.recipes = List.copyOf(newRecipes);
    }
}
