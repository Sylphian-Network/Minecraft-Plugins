package net.sylphian.minecraft.clans.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * An immutable snapshot of a single member within a clan.
 *
 * @param playerId    the member's Mojang UUID
 * @param role        the member's role; {@link ClanRole#LEADER} bypasses all permission checks
 * @param permissions the set of permissions currently granted to this member
 * @param joinedAt    when this player joined the clan
 */
public record ClanMember(
        UUID playerId,
        ClanRole role,
        Set<ClanPermission> permissions,
        Instant joinedAt
) {

    /**
     * @param permission the permission to test
     * @return {@code true} if this member holds the given permission, or is the LEADER
     */
    public boolean hasPermission(ClanPermission permission) {
        return role == ClanRole.LEADER || permissions.contains(permission);
    }
}
