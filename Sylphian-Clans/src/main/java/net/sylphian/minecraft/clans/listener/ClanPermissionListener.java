package net.sylphian.minecraft.clans.listener;

import net.sylphian.minecraft.clans.gui.ClanPermissionHolder;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Routes clicks within the clan permission GUI and prevents item movement.
 *
 * <p>All toggle logic and authority enforcement live in {@link net.sylphian.minecraft.clans.gui.ClanPermissionMenu}
 * and {@link net.sylphian.minecraft.clans.service.ClanService}; this listener only translates a
 * slot and click type into a permission toggle.</p>
 */
public final class ClanPermissionListener implements Listener {

    private static final int CLOSE_SLOT = 49;

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClanPermissionHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        ClanPermission base = holder.permissionAt(slot);
        if (base == null) {
            return;
        }

        ClanPermission permission = event.isRightClick() ? base.asGrant() : base;
        holder.getMenu().toggle(player, holder, permission);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ClanPermissionHolder) {
            event.setCancelled(true);
        }
    }
}
