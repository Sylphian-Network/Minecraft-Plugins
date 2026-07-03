package net.sylphian.minecraft.cooking.item;

import net.sylphian.minecraft.cooking.recipe.CookingRecipe;
import net.sylphian.minecraft.items.item.ItemProvider;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exposes the output items of all cooking recipes to Sylphian-Items ItemRegistry.
 *
 * <p>Items are addressable as {@code sylphian-cooking:<recipe-id>}, e.g.
 * {@code "sylphian-cooking:fish_and_chips"}.</p>
 *
 * <p>This allows other plugins to reference cooked outputs as ingredients
 * in their own systems (e.g. as crate rewards or further recipe inputs).</p>
 */
public class CookingItemProvider implements ItemProvider {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("sylphian-cooking", "item_id");

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
                .map(r -> {
                    ItemStack item = r.output().clone();
                    item.editMeta(meta -> meta.getPersistentDataContainer()
                            .set(ITEM_ID_KEY, PersistentDataType.STRING, r.id()));
                    return item;
                })
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
