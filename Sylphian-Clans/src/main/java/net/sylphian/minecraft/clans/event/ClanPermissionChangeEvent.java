package net.sylphian.minecraft.clans.event;

import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after a clan member is granted or revoked a permission.
 */
public class ClanPermissionChangeEvent extends Event {

    /** Whether the permission was added or removed. */
    public enum Action {
        /** The permission was granted to the member. */
        GRANT,
        /** The permission was revoked from the member. */
        REVOKE
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID targetUuid;
    private final ClanPermission permission;
    private final Action action;

    /**
     * @param clanId     the clan in which the change occurred
     * @param targetUuid the member whose permissions changed
     * @param permission the permission granted or revoked
     * @param action     whether it was granted or revoked
     */
    public ClanPermissionChangeEvent(UUID clanId, UUID targetUuid, ClanPermission permission, Action action) {
        this.clanId = clanId;
        this.targetUuid = targetUuid;
        this.permission = permission;
        this.action = action;
    }

    /** @return the clan in which the change occurred */
    public UUID getClanId() { return clanId; }

    /** @return the member whose permissions changed */
    public UUID getTargetUuid() { return targetUuid; }

    /** @return the permission granted or revoked */
    public ClanPermission getPermission() { return permission; }

    /** @return whether the permission was granted or revoked */
    public Action getAction() { return action; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
