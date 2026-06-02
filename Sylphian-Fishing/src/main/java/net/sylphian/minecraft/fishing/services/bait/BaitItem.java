package net.sylphian.minecraft.fishing.services.bait;

import net.sylphian.minecraft.fishing.config.BaitConfig;
import net.sylphian.minecraft.fishing.util.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for creating bait items and identifying bait projectiles.
 *
 * <p>Bait items and the snowball projectiles they launch are tagged using
 * a {@link org.bukkit.persistence.PersistentDataContainer} with the key
 * {@code sylphian:bait_type}, storing the bait ID as a string.</p>
 */
public class BaitItem {

    private BaitItem() {}

    private static NamespacedKey baitKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "bait_type");
    }

    /**
     * Creates a bait ItemStack tagged with the bait ID in its persistent data.
     *
     * @param config the bait configuration to build the item from
     * @param plugin the plugin instance for the namespaced key
     * @return the built and tagged ItemStack
     */
    public static ItemStack create(BaitConfig config, JavaPlugin plugin) {
        ItemStack item = new ItemBuilder(config.material())
                .name(config.displayName())
                .lore(
                        "<gray>Throw into water to create a bait zone.",
                        "",
                        "<aqua>Radius: <white>" + config.radius() + " blocks",
                        "<aqua>Duration: <white>" + formatDuration(config.durationSeconds()),
                        "<aqua>Bite Speed: <white>" + (int) ((1.0 - config.biteTimerMultiplier()) * 100) + "% faster"
                )
                .build();

        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(baitKey(plugin), PersistentDataType.STRING, config.id()));
        return item;
    }

    /**
     * Reads the bait ID from an ItemStack's persistent data.
     *
     * @param item   the item to check
     * @param plugin the plugin instance for the namespaced key
     * @return the bait ID, or null if the item is not a bait
     */
    public static String getBaitId(ItemStack item, JavaPlugin plugin) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(baitKey(plugin), PersistentDataType.STRING);
    }

    /**
     * Reads the bait ID from a projectile's persistent data.
     *
     * @param projectile the projectile to check
     * @param plugin     the plugin instance for the namespaced key
     * @return the bait ID, or null if not a bait projectile
     */
    public static String getBaitId(Projectile projectile, JavaPlugin plugin) {
        return projectile.getPersistentDataContainer()
                .get(baitKey(plugin), PersistentDataType.STRING);
    }

    /**
     * Tags a projectile with a bait ID in its persistent data.
     *
     * @param projectile the projectile to tag
     * @param baitId     the bait ID to store
     * @param plugin     the plugin instance for the namespaced key
     */
    public static void tagProjectile(Projectile projectile, String baitId, JavaPlugin plugin) {
        projectile.getPersistentDataContainer()
                .set(baitKey(plugin), PersistentDataType.STRING, baitId);
    }

    private static String formatDuration(int seconds) {
        return seconds >= 60
                ? (seconds / 60) + "m " + (seconds % 60) + "s"
                : seconds + "s";
    }
}