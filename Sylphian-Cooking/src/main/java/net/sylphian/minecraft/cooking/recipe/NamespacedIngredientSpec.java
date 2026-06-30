package net.sylphian.minecraft.cooking.recipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Matches a custom Sylphian-Items item by its PDC key {@code <namespace>:item_id}.
 * E.g. {@code sylphian-fishing:fish/common_cod} matches an item whose
 * {@code sylphian-fishing:item_id} equals {@code "fish/common_cod"}. The owning plugin stamps the key.
 */
public final class NamespacedIngredientSpec implements IngredientSpec {

    private final String namespacedId;
    private final NamespacedKey pdcKey;
    private final String itemId;

    /**
     * Constructs a spec for the given namespaced item ID.
     *
     * @param namespacedId the full ID, e.g. {@code "sylphian-fishing:fish/common_cod"}
     */
    public NamespacedIngredientSpec(String namespacedId) {
        this.namespacedId = namespacedId;

        int colon = namespacedId.indexOf(':');
        String pluginNamespace = namespacedId.substring(0, colon);
        this.itemId = namespacedId.substring(colon + 1);
        this.pdcKey = new NamespacedKey(pluginNamespace, "item_id");
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String stamped = meta.getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
        return itemId.equals(stamped);
    }

    @Override
    public String displayId() {
        return namespacedId;
    }
}
