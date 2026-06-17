package net.sylphian.minecraft.clans.model;

/**
 * A capability that can be granted to or revoked from an individual clan member.
 *
 * <p>Each capability permission has a paired {@code GRANT_*} counterpart that
 * controls who may assign or revoke that capability. Only the clan LEADER may
 * assign {@code GRANT_*} permissions; a member holding {@code GRANT_X} may
 * assign or revoke {@code X} on other members.</p>
 *
 * <p>Use {@link #isGrant()}, {@link #asGrant()}, and {@link #asBase()} to
 * navigate between a permission and its counterpart without string manipulation
 * at call sites.</p>
 */
public enum ClanPermission {

    /** Can break blocks in chunks claimed by this clan. */
    BREAK_BLOCKS,
    /** Can grant or revoke {@link #BREAK_BLOCKS} on other members. */
    GRANT_BREAK_BLOCKS,

    /** Can place blocks in chunks claimed by this clan. */
    PLACE_BLOCKS,
    /** Can grant or revoke {@link #PLACE_BLOCKS} on other members. */
    GRANT_PLACE_BLOCKS,

    /** Can kill passive animals in chunks claimed by this clan. */
    KILL_ANIMALS,
    /** Can grant or revoke {@link #KILL_ANIMALS} on other members. */
    GRANT_KILL_ANIMALS,

    /** Can kill hostile monsters in chunks claimed by this clan. */
    KILL_MONSTERS,
    /** Can grant or revoke {@link #KILL_MONSTERS} on other members. */
    GRANT_KILL_MONSTERS,

    /** Can open containers (chests, barrels, etc.) in chunks claimed by this clan. */
    OPEN_CONTAINERS,
    /** Can grant or revoke {@link #OPEN_CONTAINERS} on other members. */
    GRANT_OPEN_CONTAINERS,

    /** Can use doors, gates, and trapdoors in chunks claimed by this clan. */
    USE_DOORS,
    /** Can grant or revoke {@link #USE_DOORS} on other members. */
    GRANT_USE_DOORS,

    /** Can interact with buttons, levers, crafting tables, etc. in chunks claimed by this clan. */
    INTERACT,
    /** Can grant or revoke {@link #INTERACT} on other members. */
    GRANT_INTERACT,

    /** Can invite players to the clan. */
    INVITE_MEMBERS,
    /** Can grant or revoke {@link #INVITE_MEMBERS} on other members. */
    GRANT_INVITE_MEMBERS,

    /** Can kick members from the clan. Cannot be used against the LEADER. */
    KICK_MEMBERS,
    /** Can grant or revoke {@link #KICK_MEMBERS} on other members. */
    GRANT_KICK_MEMBERS,

    /** Can claim chunks on behalf of the clan. */
    CLAIM_TERRITORY,
    /** Can grant or revoke {@link #CLAIM_TERRITORY} on other members. */
    GRANT_CLAIM_TERRITORY,

    /** Can unclaim chunks on behalf of the clan. */
    UNCLAIM_TERRITORY,
    /** Can grant or revoke {@link #UNCLAIM_TERRITORY} on other members. */
    GRANT_UNCLAIM_TERRITORY,

    /** Can set the clan's home location. */
    SET_HOME,
    /** Can grant or revoke {@link #SET_HOME} on other members. */
    GRANT_SET_HOME,

    /** Can set the clan's message of the day. */
    SET_MOTD,
    /** Can grant or revoke {@link #SET_MOTD} on other members. */
    GRANT_SET_MOTD;

    /**
     * @return {@code true} if this is a {@code GRANT_*} permission
     */
    public boolean isGrant() {
        return name().startsWith("GRANT_");
    }

    /**
     * Returns the {@code GRANT_*} counterpart of this permission.
     * If this is already a {@code GRANT_*} permission, returns itself.
     *
     * @return the grant counterpart, e.g. {@code BREAK_BLOCKS → GRANT_BREAK_BLOCKS}
     */
    public ClanPermission asGrant() {
        return isGrant() ? this : valueOf("GRANT_" + name());
    }

    /**
     * Returns the base capability permission for this {@code GRANT_*} entry.
     * If this is already a base permission, returns itself.
     *
     * @return the base permission, e.g. {@code GRANT_BREAK_BLOCKS → BREAK_BLOCKS}
     */
    public ClanPermission asBase() {
        return isGrant() ? valueOf(name().substring(6)) : this;
    }
}

