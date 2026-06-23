package net.sylphian.minecraft.clans.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import net.sylphian.minecraft.clans.gui.ClanWarpAccessHolder;
import net.sylphian.minecraft.clans.gui.ClanWarpAccessMenu;
import net.sylphian.minecraft.clans.gui.ClanWarpHolder;
import net.sylphian.minecraft.clans.gui.ClanWarpMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.UUID;

/**
 * Routes clicks for the warp list GUI and the per-warp access editor, and blocks item movement.
 */
public final class ClanWarpListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder instanceof ClanWarpHolder warpHolder) {
            handleWarpList(event, warpHolder);
        } else if (holder instanceof ClanWarpAccessHolder accessHolder) {
            handleAccess(event, accessHolder);
        }
    }

    private void handleWarpList(InventoryClickEvent event, ClanWarpHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == ClanWarpMenu.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        ClanWarpModel warp = holder.warpAt(slot);
        if (warp == null) {
            return;
        }
        if (event.isRightClick()) {
            if (holder.canManage()) {
                holder.getMenu().openAccess(player, warp);
            }
            return;
        }
        if (holder.isUsable(slot)) {
            holder.getMenu().teleport(player, warp);
        } else {
            player.sendMessage(Component.text("You don't have access to that warp.", NamedTextColor.RED));
        }
    }

    private void handleAccess(InventoryClickEvent event, ClanWarpAccessHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == ClanWarpAccessMenu.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == ClanWarpAccessMenu.RESTRICT_SLOT) {
            holder.getMenu().toggleRestricted(player, holder);
            return;
        }
        UUID memberId = holder.memberAt(slot);
        if (memberId != null) {
            holder.getMenu().toggleAccess(player, holder, memberId);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Object holder = event.getInventory().getHolder();
        if (holder instanceof ClanWarpHolder || holder instanceof ClanWarpAccessHolder) {
            event.setCancelled(true);
        }
    }
}
