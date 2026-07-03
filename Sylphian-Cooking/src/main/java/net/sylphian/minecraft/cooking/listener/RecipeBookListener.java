package net.sylphian.minecraft.cooking.listener;

import net.sylphian.minecraft.cooking.gui.RecipeBookHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles inventory events within the recipe book GUI.
 * Prevents item movement and drives prev/next page navigation.
 */
public final class RecipeBookListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecipeBookHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (slot == 48) {
            holder.getMenu().open(player, holder.getPage() - 1);
        } else if (slot == 50) {
            holder.getMenu().open(player, holder.getPage() + 1);
        }
    }
}
