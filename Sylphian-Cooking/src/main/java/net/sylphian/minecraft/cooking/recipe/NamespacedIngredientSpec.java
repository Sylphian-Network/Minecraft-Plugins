package net.sylphian.minecraft.cooking.recipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Matches a custom item registered via Sylphian-Core's ItemRegistry by checking
 * the PDC key {@code <plugin-namespace>:item_id} stamped on the ItemStack.
 *
 * <p>For example, an ingredient spec for {@code sylphian-fishing:fish/common_cod}
 * will match any ItemStack that has the PDC key {@code sylphian-fishing:item_id}
 * set to {@code "fish/common_cod"}.</p>
 *
 * <p>The owning plugin is responsible for stamping this key when it builds the
 * item. Sylphian-Fishing stamps {@code sylphian-fishing:item_id} on all fish items.</p>
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
