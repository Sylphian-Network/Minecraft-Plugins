package net.sylphian.minecraft.clans.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Inventory holder for the per-warp access editor GUI.
 */
public class ClanWarpAccessHolder implements InventoryHolder {

    private final ClanWarpAccessMenu menu;
    private final UUID viewerId;
    private final UUID clanId;
    private final String warpName;
    private final Map<Integer, UUID> slotToMember;

    /**
     * @param menu         the menu that built this inventory
     * @param viewerId     the player managing access
     * @param clanId       the owning clan
     * @param warpName     the warp being edited
     * @param slotToMember maps a member-head slot to that member's UUID (toggle-able members only)
     */
    public ClanWarpAccessHolder(ClanWarpAccessMenu menu, UUID viewerId, UUID clanId, String warpName,
                                Map<Integer, UUID> slotToMember) {
        this.menu = menu;
        this.viewerId = viewerId;
        this.clanId = clanId;
        this.warpName = warpName;
        this.slotToMember = slotToMember;
    }

    public ClanWarpAccessMenu getMenu() {
        return menu;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public UUID getClanId() {
        return clanId;
    }

    public String getWarpName() {
        return warpName;
    }

    public @Nullable UUID memberAt(int slot) {
        return slotToMember.get(slot);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}
