package net.sylphian.minecraft.core.item;

import org.bukkit.inventory.ItemStack;
import java.util.Optional;
import java.util.Set;

/**
 * Implemented by any plugin that wants to expose custom items to other plugins.
 * Register an implementation with {@link ItemRegistry} on enable and unregister on disable.
 */
public interface ItemProvider {

    /**
     * Returns the namespace for this provider, e.g. {@code "sylphian-crates"}.
     * Must be unique across all registered providers.
     *
     * @return the namespace
     */
    String namespace();

    /**
     * Builds the item for the given item ID within this provider's namespace.
     *
     * @param itemId the item identifier, e.g. {@code "legendary_key"}
     * @return the built ItemStack, or empty if the ID is not recognised
     */
    Optional<ItemStack> provide(String itemId);

    /**
     * Returns all item IDs this provider can build.
     * Used for config validation and tab completion.
     *
     * @return the set of registered item IDs
     */
    Set<String> itemIds();
}