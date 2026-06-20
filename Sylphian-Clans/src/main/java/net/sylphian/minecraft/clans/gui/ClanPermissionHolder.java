package net.sylphian.minecraft.clans.gui;

import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Inventory holder identifying an open clan permission GUI.
 *
 * <p>Carries only identifiers and a slot-to-permission routing map; it holds no
 * mutable permission state. The current state is always re-read from the cache
 * when the menu is (re)built, keeping the database as the single source of truth.</p>
 */
public class ClanPermissionHolder implements InventoryHolder {

    private final ClanPermissionMenu menu;
    private final UUID viewerId;
    private final UUID targetId;
    private final UUID clanId;
    private final Map<Integer, ClanPermission> slotToPermission;

    /**
     * @param menu             the menu that built this inventory, used to rebuild on toggle
     * @param viewerId         the player viewing the GUI
     * @param targetId         the member whose permissions are being edited
     * @param clanId           the clan both players belong to
     * @param slotToPermission maps a capability slot to its base {@link ClanPermission}
     */
    public ClanPermissionHolder(ClanPermissionMenu menu, UUID viewerId, UUID targetId, UUID clanId,
                                Map<Integer, ClanPermission> slotToPermission) {
        this.menu = menu;
        this.viewerId = viewerId;
        this.targetId = targetId;
        this.clanId = clanId;
        this.slotToPermission = slotToPermission;
    }

    public ClanPermissionMenu getMenu() {
        return menu;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getClanId() {
        return clanId;
    }

    /**
     * @param slot the raw slot clicked
     * @return the base capability mapped to that slot, or {@code null} if the slot is not a capability icon
     */
    public @Nullable ClanPermission permissionAt(int slot) {
        return slotToPermission.get(slot);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public @Nullable Inventory getInventory() {
        return null;
    }
}
