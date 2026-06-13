package net.sylphian.minecraft.crates.gui.opening;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.crates.config.CrateConfig;
import net.sylphian.minecraft.items.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Colorful glass-pane picker opening style.
 *
 * <p>27 randomly coloured glass panes fill the inventory. The player has
 * {@code playerPicks} clicks — each click rolls and immediately grants a
 * reward from the crate's weighted pool. All panes guarantee a reward.</p>
 */
public class ColorfulGUI {

    private static final int SIZE = 27;

    private static final Material[] PANE_COLORS = {
            Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.GRAY_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE
    };

    private ColorfulGUI() {}

    /**
     * Opens the colorful pane picker for the given player.
     *
     * @param player        the player opening the crate
     * @param crate         the crate configuration
     * @param picks         number of panes the player may click
     */
    public static void open(Player player, CrateConfig crate, int picks) {
        List<Material> colors = new ArrayList<>(List.of(PANE_COLORS));
        while (colors.size() < SIZE) colors.addAll(List.of(PANE_COLORS));
        Collections.shuffle(colors);

        ColorfulGUIHolder holder = new ColorfulGUIHolder(crate, picks);
        Component title = Component.text("Choose Your Reward", NamedTextColor.DARK_GRAY);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        String picksLabel = picks + " pick" + (picks == 1 ? "" : "s") + " remaining";
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, new ItemBuilder(colors.get(i))
                    .name("<gray>? <dark_gray>(" + picksLabel + ")")
                    .build());
        }

        player.openInventory(inv);
    }
}