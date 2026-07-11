package net.sylphian.minecraft.entities.item;

import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Soft-dependency bridge to Sylphian-Items. Resolves a {@code namespace:id}
 * reference to a built ItemStack, returning empty when Sylphian-Items is absent.
 * Every reference to the optional plugin's classes is isolated here so the module
 * links cleanly without it.
 */
public final class ItemsBridge {

    private ItemsBridge() {}

    /**
     * Resolves a namespaced custom-item reference through the ItemRegistry.
     *
     * @param namespacedId the item reference in the form {@code "namespace:item-id"}
     * @return the built item, or empty if Sylphian-Items is absent or the ID is unknown
     */
    public static Optional<ItemStack> resolve(String namespacedId) {
        if (Bukkit.getPluginManager().getPlugin("Sylphian-Items") == null) return Optional.empty();
        return ItemRegistry.get(namespacedId);
    }
}
