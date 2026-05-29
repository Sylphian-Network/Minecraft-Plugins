package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.gui.EncyclopaediaHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EncyclopaediaListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EncyclopaediaHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 48) {
            if (holder.getPage() > 0) {
                holder.getMenu().open(player, holder.getPage() - 1);
            }
        }

        if (slot == 50) {
            holder.getMenu().open(player, holder.getPage() + 1);
        }
    }
}