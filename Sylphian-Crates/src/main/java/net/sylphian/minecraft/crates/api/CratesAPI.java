package net.sylphian.minecraft.crates.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Public API for Sylphian-Crates.
 *
 * <p>External plugins should access this via Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * rather than depending on the full plugin jar:</p>
 *
 * <pre>{@code
 * RegisteredServiceProvider<CratesAPI> provider =
 *     Bukkit.getServicesManager().getRegistration(CratesAPI.class);
 * if (provider != null) {
 *     provider.getProvider().giveKey(player, "legendary_key");
 * }
 * }</pre>
 */
public interface CratesAPI {

    /**
     * Gives the player a physical crate key item.
     * The key is added to the player's inventory, or dropped at their feet if full.
     *
     * @param player the player to give the key to
     * @param keyId  the key ID as defined in keys.yml
     * @throws IllegalArgumentException if the key ID is not registered
     */
    void giveKey(Player player, String keyId);

    /**
     * Builds the physical ItemStack for a crate key, including its NBT tag.
     * Use this when you need the item itself rather than giving it directly,
     * for example to set it as a caught fishing item.
     *
     * @param keyId the key ID as defined in keys.yml
     * @return the built and tagged ItemStack
     * @throws IllegalArgumentException if the key ID is not registered
     */
    ItemStack buildKey(String keyId);
}