package net.sylphian.minecraft.gathering.bridge;

import net.sylphian.minecraft.items.item.ItemRegistry;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Isolates every reference to Sylphian-Items so item resolution goes through one place.
 */
public final class ItemsBridge {

    private ItemsBridge() {}

    /**
     * Resolves a namespaced item reference to a fresh stack.
     *
     * @param itemId the reference in {@code "namespace:id"} form
     * @return the built item, or empty if unknown
     */
    public static Optional<ItemStack> resolve(String itemId) {
        return ItemRegistry.get(itemId);
    }
}
