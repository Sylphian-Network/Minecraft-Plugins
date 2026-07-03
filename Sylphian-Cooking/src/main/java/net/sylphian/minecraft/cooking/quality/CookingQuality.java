package net.sylphian.minecraft.cooking.quality;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The four quality tiers a cooking recipe can produce.
 * Each tier carries an XP multiplier and a lore line applied to the output item.
 */
public enum CookingQuality {

    BURNT(0.5),
    PLAIN(1.0),
    GOOD(1.5),
    PERFECT(2.0);

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final double xpMultiplier;

    CookingQuality(double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    /**
     * The multiplier applied to base recipe XP when this tier is rolled.
     *
     * @return XP multiplier, e.g. {@code 2.0} for Perfect
     */
    public double xpMultiplier() {
        return xpMultiplier;
    }

    /**
     * Clones the base item and appends this tier's lore line.
     *
     * @param base   the recipe's base output item; not modified
     * @param format the presentation rules for this tier
     * @return the formatted item
     */
    public ItemStack applyTo(ItemStack base, QualityFormat format) {
        ItemStack result = base.clone();

        result.editMeta(meta -> {
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() && meta.lore() != null
                    ? new ArrayList<>(meta.lore())
                    : new ArrayList<>();
            lore.add(MINI.deserialize(format.loreLine())
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(lore);
        });

        return result;
    }
}
