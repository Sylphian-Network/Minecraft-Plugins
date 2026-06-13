package net.sylphian.minecraft.items.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for building ItemStacks with fluent API.
 * Simplifies setting display names, lore, and other item metadata
 * using MiniMessage for universal text formatting.
 */
public class ItemBuilder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ItemStack item;
    private final ItemMeta meta;

    /**
     * Constructs a new ItemBuilder for the specified material.
     *
     * @param material the item material
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /**
     * Sets the display name of the item using an Adventure Component.
     * Removes italic decoration if not explicitly set.
     *
     * @param name the component name
     * @return the builder instance
     */
    public ItemBuilder name(Component name) {
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return this;
    }

    /**
     * Sets the display name of the item using a MiniMessage string.
     *
     * @param miniMessage the MiniMessage name string
     * @return the builder instance
     */
    public ItemBuilder name(String miniMessage) {
        return name(MINI.deserialize(miniMessage));
    }

    /**
     * Sets the lore of the item using a list of Adventure Components.
     * Removes italic decoration from each line if not explicitly set.
     *
     * @param lore the list of components
     * @return the builder instance
     */
    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore.stream()
                .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        return this;
    }

    /**
     * Sets the lore of the item using multiple MiniMessage strings.
     *
     * @param lines the MiniMessage lore lines
     * @return the builder instance
     */
    public ItemBuilder lore(String... lines) {
        return lore(Arrays.stream(lines)
                .map(MINI::deserialize)
                .toList());
    }

    /**
     * Sets the lore of the item using a list of MiniMessage strings.
     *
     * @param lines the list of MiniMessage lore lines
     * @return the builder instance
     */
    public ItemBuilder loreStrings(List<String> lines) {
        return lore(lines.stream()
                .map(MINI::deserialize)
                .toList());
    }

    /**
     * Applies an enchantment to the item by namespaced key.
     * Accepts both plain keys (e.g. {@code "sharpness"}, resolved as {@code minecraft:sharpness})
     * and fully qualified namespaced keys (e.g. {@code "sylphian:super_fish"}).
     *
     * @param key   the enchantment key, with or without namespace
     * @param level the enchantment level
     * @return the builder instance
     */
    public ItemBuilder enchant(String key, int level) {
        NamespacedKey namespacedKey = key.contains(":")
                ? NamespacedKey.fromString(key.toLowerCase())
                : NamespacedKey.minecraft(key.toLowerCase());

        if (namespacedKey == null) return this;

        Enchantment enchantment = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(namespacedKey);

        if (enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }

        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder customModelData(float value) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of(value));
        meta.setCustomModelDataComponent(component);
        return this;
    }

    /**
     * Finalizes the item building process.
     *
     * @return the built ItemStack
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}