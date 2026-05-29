package net.sylphian.minecraft.fishing;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

public class SylphianFishingBootstrap implements PluginBootstrap {

    public static final Key SUPER_FISH_KEY = Key.key("sylphian:super_fish");

    @Override
    public void bootstrap(BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> lifecycle = context.getLifecycleManager();

        lifecycle.registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
                    event.registry().register(
                            EnchantmentKeys.create(SUPER_FISH_KEY),
                            (EnchantmentRegistryEntry.Builder builder) -> {
                                builder
                                        .description(Component.text("Super Fish"))
                                        .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.FISHES))
                                        .weight(4)
                                        .maxLevel(1)
                                        .anvilCost(1)
                                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(5, 0))
                                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(12, 0))
                                        .activeSlots(EquipmentSlotGroup.MAINHAND);
                            }
                    );
                })
        );
    }
}