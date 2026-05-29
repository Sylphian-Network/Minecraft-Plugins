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

public class SuperFishEnchantmentListener implements Listener {
    
    private final ConfigLoader config;
    private final Enchantment superFishEnchantment;

    public SuperFishEnchantmentListener(ConfigLoader config) {
        this.config = config;
        this.superFishEnchantment = registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(SUPER_FISH_KEY);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (superFishEnchantment == null) return;
        if (!event.getItem().getEnchantments().containsKey(superFishEnchantment)) return;

        applySuperFishEffects(event.getPlayer());
    }

    private void applySuperFishEffects(Player player) {
        player.addPotionEffects(config.getMutationEffects("super_fish"));
    }
}