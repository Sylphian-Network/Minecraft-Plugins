package net.sylphian.minecraft.fishing.listeners;

import io.papermc.paper.registry.RegistryKey;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import static io.papermc.paper.registry.RegistryAccess.registryAccess;
import static net.sylphian.minecraft.fishing.SylphianFishingBootstrap.SUPER_FISH_KEY;

/**
 * Listener for consumption of items with the Super Fish enchantment.
 * Applies configured potion effects to the player when they eat a Super Fish.
 */
public class SuperFishEnchantmentListener implements Listener {
    
    private final ConfigLoader config;
    private final Enchantment superFishEnchantment;

    /**
     * Constructs a new SuperFishEnchantmentListener.
     *
     * @param config the configuration loader
     */
    public SuperFishEnchantmentListener(ConfigLoader config) {
        this.config = config;
        this.superFishEnchantment = registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(SUPER_FISH_KEY);
    }

    /**
     * Handles the consumption of a fish.
     * If the fish has the Super Fish enchantment, it applies bonus effects.
     *
     * @param event the item consume event
     */
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (superFishEnchantment == null) return;
        if (!event.getItem().getEnchantments().containsKey(superFishEnchantment)) return;

        applySuperFishEffects(event.getPlayer());
    }

    /**
     * Applies the configured mutation effects for "super_fish" to the player.
     *
     * @param player the player who consumed the fish
     */
    private void applySuperFishEffects(Player player) {
        player.addPotionEffects(config.getMutationConfig("super_fish").effects());
    }
}