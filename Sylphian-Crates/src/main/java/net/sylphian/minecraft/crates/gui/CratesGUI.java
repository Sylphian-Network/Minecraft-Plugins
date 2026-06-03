package net.sylphian.minecraft.crates.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Builds and opens the main crates GUI for a player.
 *
 * <p>The GUI is a 27-slot inventory laid out as follows:</p>
 * <pre>
 *  [ ][ ][ ][ ][ ][ ][ ][ ][ ]
 *  [ ][ ][K][ ][ ][ ][C][ ][ ]
 *  [ ][ ][ ][ ][ ][ ][ ][ ][ ]
 * </pre>
 * <p>Slot 11 (K) is the key slot where the player places their key.
 * Slot 15 (C) auto-populates with the crate's display item once a valid key is placed.
 * All other slots are filled with decorative glass panes.</p>
 */
public class CratesGUI {

    /** The inventory slot the player places their key into. */
    public static final int KEY_SLOT = 11;

    /** The inventory slot that displays the crate once a key is placed. */
    public static final int CRATE_SLOT = 15;

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private CratesGUI() {}

    /**
     * Opens the crates GUI for the given player.
     *
     * @param player the player to open the GUI for
     */
    public static void open(Player player) {
        CratesGUIHolder holder = new CratesGUIHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MINI.deserialize("<dark_gray>Crates"));
        holder.setInventory(inv);

        fillBorder(inv);
        restoreKeySlot(inv);
        restoreCrateSlot(inv);

        player.openInventory(inv);
    }

    /**
     * Fills all non-functional slots with a decorative gray glass pane.
     *
     * @param inv the inventory to fill
     */
    private static void fillBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(
                MINI.deserialize(" ").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));

        for (int i = 0; i < inv.getSize(); i++) {
            if (i != KEY_SLOT && i != CRATE_SLOT) {
                inv.setItem(i, filler);
            }
        }
    }

    /**
     * Restores the key slot placeholder. Call when a key is removed from the slot.
     *
     * @param inv the GUI inventory
     */
    public static void restoreKeySlot(Inventory inv) {
        ItemStack placeholder = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        placeholder.editMeta(meta -> meta.displayName(
                MINI.deserialize("<green>Place Key Here")
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        inv.setItem(KEY_SLOT, placeholder);
    }

    /**
     * Sets the crate slot placeholder shown when no key has been placed.
     *
     * @param inv the GUI inventory
     */
    public static void restoreCrateSlot(Inventory inv) {
        ItemStack placeholder = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        placeholder.editMeta(meta -> meta.displayName(
                MINI.deserialize("<red>No Key Placed")
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
        inv.setItem(CRATE_SLOT, placeholder);
    }
}