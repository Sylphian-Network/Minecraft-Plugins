package net.sylphian.minecraft.skills.listener;

import net.sylphian.minecraft.skills.gui.SkillDetailHolder;
import net.sylphian.minecraft.skills.gui.SkillsMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles click and drag events inside the skills GUI inventories.
 *
 * <p>Menu detection uses the inventory holder: {@link SkillsMenuHolder} for the
 * category list and {@link SkillDetailHolder} for the ability detail view. No title
 * matching or PDC keys are required.</p>
 *
 * <p>All clicks and drags inside either menu are cancelled to prevent item movement.</p>
 */
public final class SkillsMenuListener implements Listener {

    /**
     * Routes clicks in the category list to open the detail view, and clicks on the
     * back button in the detail view to return to the category list.
     */
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SkillsMenuHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            var skill = holder.getSkillAt(event.getRawSlot());
            if (skill != null) holder.getMenu().openDetail(player, skill);

        } else if (event.getInventory().getHolder() instanceof SkillDetailHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getRawSlot() == SkillDetailHolder.BACK_SLOT) holder.getMenu().open(player);
        }
    }

    /** Cancels all drag events inside either menu to prevent item movement. */
    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SkillsMenuHolder
                || event.getInventory().getHolder() instanceof SkillDetailHolder) {
            event.setCancelled(true);
        }
    }
}
