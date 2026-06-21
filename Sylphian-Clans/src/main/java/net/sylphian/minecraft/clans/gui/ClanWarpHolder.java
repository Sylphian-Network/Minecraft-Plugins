package net.sylphian.minecraft.clans.gui;

import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Inventory holder for the clan warp list GUI.
 *
 * <p>Carries only identifiers and the slot-to-warp routing for the rendered page;
 * warp state is always re-read when the menu is rebuilt.</p>
 */
public class ClanWarpHolder implements InventoryHolder {

    private final ClanWarpMenu menu;
    private final UUID viewerId;
    private final UUID clanId;
    private final boolean canManage;
    private final Map<Integer, ClanWarpModel> slotToWarp;
    private final Set<Integer> usableSlots;

    /**
     * @param menu        the menu that built this inventory
     * @param viewerId    the player viewing the GUI
     * @param clanId      the viewer's clan
     * @param canManage   whether the viewer holds MANAGE_WARP (enables right-click management)
     * @param slotToWarp  maps a slot to the warp shown there
     * @param usableSlots the slots whose warp the viewer may teleport to
     */
    public ClanWarpHolder(ClanWarpMenu menu, UUID viewerId, UUID clanId, boolean canManage,
                          Map<Integer, ClanWarpModel> slotToWarp, Set<Integer> usableSlots) {
        this.menu = menu;
        this.viewerId = viewerId;
        this.clanId = clanId;
        this.canManage = canManage;
        this.slotToWarp = slotToWarp;
        this.usableSlots = usableSlots;
    }

    public ClanWarpMenu getMenu() {
        return menu;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public UUID getClanId() {
        return clanId;
    }

    public boolean canManage() {
        return canManage;
    }

    public @Nullable ClanWarpModel warpAt(int slot) {
        return slotToWarp.get(slot);
    }

    public boolean isUsable(int slot) {
        return usableSlots.contains(slot);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}
