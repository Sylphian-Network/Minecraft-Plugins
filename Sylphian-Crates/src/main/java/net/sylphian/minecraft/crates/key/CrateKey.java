package net.sylphian.minecraft.crates.key;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.crates.config.KeyConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

/**
 * Utility class for creating physical crate key items and reading their identity.
 *
 * <p>Keys are tagged using a {@link org.bukkit.persistence.PersistentDataContainer}
 * with the key {@code sylphian:crate_key_id}, storing the key ID as a string.
 * This allows the crates GUI to identify which crate a placed key opens.</p>
 */
public class CrateKey {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private CrateKey() {}

    private static NamespacedKey keyId(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "crate_key_id");
    }

    /**
     * Creates a physical key ItemStack tagged with the key ID in its persistent data.
     *
     * @param config the key configuration to build the item from
     * @param plugin the plugin instance for the namespaced key
     * @return the built and tagged ItemStack
     */
    public static ItemStack create(KeyConfig config, JavaPlugin plugin) {
        ItemStack item = new ItemStack(config.material());
        item.editMeta(meta -> {
            meta.displayName(MINI.deserialize(config.displayName())
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(config.lore().stream()
                    .map(line -> MINI.deserialize(line)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .collect(Collectors.toList()));
            meta.getPersistentDataContainer()
                    .set(keyId(plugin), PersistentDataType.STRING, config.id());
        });
        return item;
    }

    /**
     * Reads the key ID from an ItemStack's persistent data.
     *
     * @param item   the item to check
     * @param plugin the plugin instance for the namespaced key
     * @return the key ID, or null if the item is not a crate key
     */
    @Nullable
    public static String getKeyId(ItemStack item, JavaPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(keyId(plugin), PersistentDataType.STRING);
    }
}