package net.sylphian.minecraft.fishing.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(Component name) {
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        return name(MINI.deserialize(miniMessage));
    }

    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore.stream()
                .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.stream(lines)
                .map(MINI::deserialize)
                .toList());
    }

    public ItemBuilder loreStrings(List<String> lines) {
        return lore(lines.stream()
                .map(MINI::deserialize)
                .toList());
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

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}