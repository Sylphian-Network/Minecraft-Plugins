package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.gui.EncyclopaediaHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listener for inventory events within the fish encyclopaedia GUI.
 * Handles navigation (next/previous page) and prevents item movement.
 */
public class EncyclopaediaListener implements Listener {

    /**
     * Handles clicks within the encyclopaedia inventory.
     * Prevents taking items and handles page navigation.
     *
     * @param event the inventory click event
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EncyclopaediaHolder holder)) return;

        // Cancel the event to prevent players from taking items out of the encyclopaedia
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        // Slot 48 is the "Previous Page" button
        if (slot == 48) {
            if (holder.getPage() > 0) {
                holder.getMenu().open(player, holder.getPage() - 1);
            }
        }

        // Slot 50 is the "Next Page" button
        if (slot == 50) {
            holder.getMenu().open(player, holder.getPage() + 1);
        }
    }
}