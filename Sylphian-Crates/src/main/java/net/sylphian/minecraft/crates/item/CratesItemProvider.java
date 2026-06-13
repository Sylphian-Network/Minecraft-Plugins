package net.sylphian.minecraft.crates.item;

import net.sylphian.minecraft.items.item.ItemProvider;
import net.sylphian.minecraft.crates.SylphianCrates;
import net.sylphian.minecraft.crates.config.KeyConfig;
import net.sylphian.minecraft.crates.key.CrateKey;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

/**
 * Exposes Sylphian-Crates items to the cross-plugin item registry.
 * Currently provides crate keys, referenced as {@code sylphian-crates:<key-id>}.
 */
public class CratesItemProvider implements ItemProvider {

    private final SylphianCrates plugin;

    public CratesItemProvider(SylphianCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public String namespace() { return "sylphian-crates"; }

    @Override
    public Optional<ItemStack> provide(String itemId) {
        KeyConfig key = plugin.getKeys().get(itemId);
        if (key == null) return Optional.empty();
        return Optional.of(CrateKey.create(key, plugin));
    }

    @Override
    public Set<String> itemIds() {
        return plugin.getKeys().keySet();
    }
}