package net.sylphian.minecraft.crates.api;

import net.sylphian.minecraft.crates.config.KeyConfig;
import net.sylphian.minecraft.crates.key.CrateKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Default implementation of {@link CratesAPI}.
 * Registered with Bukkit's {@link org.bukkit.plugin.ServicesManager} on enable.
 */
public class CratesAPIImpl implements CratesAPI {

    private final Map<String, KeyConfig> keys;
    private final JavaPlugin plugin;

    /**
     * Constructs a new CratesAPIImpl.
     *
     * @param keys   the loaded key configurations
     * @param plugin the plugin instance for NBT key generation
     */
    public CratesAPIImpl(Map<String, KeyConfig> keys, JavaPlugin plugin) {
        this.keys = keys;
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code keyId} is not a registered key
     */
    @Override
    public void giveKey(Player player, String keyId) {
        KeyConfig config = keys.get(keyId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown crate key ID: '" + keyId + "'");
        }
        ItemStack key = CrateKey.create(config, plugin);
        player.getInventory().addItem(key).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code keyId} is not a registered key
     */
    @Override
    public ItemStack buildKey(String keyId) {
        KeyConfig config = keys.get(keyId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown crate key ID: '" + keyId + "'");
        }
        return CrateKey.create(config, plugin);
    }
}