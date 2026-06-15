package net.sylphian.minecraft.clans.model;

/**
 * The role held by a member within a clan.
 *
 * <p>Role is used solely to identify the clan owner. All in-game capabilities
 * are controlled by {@link ClanPermission}, not by role.</p>
 */
public enum ClanRole {

    /**
     * The clan owner. Implicitly bypasses all permission checks and is the only
     * member who can assign or revoke {@code GRANT_*} permissions.
     */
    LEADER,

    /**
     * A standard clan member whose capabilities are determined entirely by their
     * assigned {@link ClanPermission} set.
     */
    MEMBER
}
