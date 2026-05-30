package net.sylphian.minecraft.fishing.mutation.impl;

import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.mutation.FishContext;
import net.sylphian.minecraft.fishing.mutation.FishMutation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.papermc.paper.registry.RegistryAccess.registryAccess;
import static net.sylphian.minecraft.fishing.SylphianFishingBootstrap.SUPER_FISH_KEY;

/**
 * A mutation that applies the Super Fish enchantment to a caught fish.
 * The enchantment is hidden from the tooltip using {@link ItemFlag#HIDE_ENCHANTS}
 * and replaced with a lore line so it appears in the correct position
 * relative to the fish's description, rarity, and weight.
 *
 * <p>The enchantment reference is resolved once at construction time
 * rather than on every catch to avoid repeated registry lookups.</p>
 */
public class SuperFishMutation implements FishMutation {

    /**
     * The Super Fish enchantment resolved from the Paper registry at construction.
     * May be null if the enchantment was not registered correctly by the bootstrapper.
     */
    private final Enchantment superFish;

    /**
     * Constructs a new SuperFishMutation and resolves the Super Fish enchantment
     * from the Paper registry using the key defined in {@link net.sylphian.minecraft.fishing.SylphianFishingBootstrap}.
     */
    public SuperFishMutation() {
        this.superFish = registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(SUPER_FISH_KEY);
    }

    /**
     * Always returns true as Super Fish can apply to any catch context.
     *
     * @param player  the player who caught the fish
     * @param context the catch context
     * @return true
     */
    @Override
    public boolean shouldApply(Player player, FishContext context) {
        return true;
    }

    /**
     * Applies the Super Fish enchantment to the item and appends a mutation
     * lore line to the item's existing lore.
     *
     * <p>The real enchantment is hidden via {@link ItemFlag#HIDE_ENCHANTS} so it
     * does not render between the display name and lore. A styled lore line
     * is appended instead to maintain correct tooltip ordering.</p>
     *
     * @param item    the fish item stack to mutate
     * @param player  the player who caught the fish
     * @param context the catch context
     */
    @Override
    public void apply(ItemStack item, Player player, FishContext context) {
        if (superFish == null) return;

        item.addUnsafeEnchantment(superFish, 1);

        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> currentLore = meta.lore() != null
                ? new ArrayList<>(Objects.requireNonNull(meta.lore()))
                : new ArrayList<>();

        currentLore.add(MiniMessage.miniMessage().deserialize(
                "<gray>Mutation: <aqua>Super Fish"
        ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        meta.lore(currentLore);
        item.setItemMeta(meta);
    }
}
