package net.sylphian.minecraft.clans.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable snapshot of a clan and its membership at a point in time.
 *
 * <p>Derived state (leader UUID, member lookups, permission checks) is computed
 * from the {@code members} list rather than stored separately, keeping the
 * record self-consistent.</p>
 *
 * @param clanId    the clan's unique identifier
 * @param name      the clan's display name (unique across all clans)
 * @param tag       the short tag displayed in chat and on the sidebar, e.g. {@code SYL}
 * @param members   all current members, including the leader
 * @param createdAt when the clan was founded
 */
public record Clan(
        UUID clanId,
        String name,
        String tag,
        List<ClanMember> members,
        Instant createdAt
) {

    /**
     * Returns the member record for the given player, if they are in this clan.
     *
     * @param playerUuid the player to look up
     * @return the member record, or empty if the player is not a member
     */
    public Optional<ClanMember> getMember(UUID playerUuid) {
        return members.stream()
                .filter(m -> m.playerId().equals(playerUuid))
                .findFirst();
    }

    /**
     * @param playerUuid the player to test
     * @return {@code true} if the player is a member of this clan
     */
    public boolean isMember(UUID playerUuid) {
        return getMember(playerUuid).isPresent();
    }

    /**
     * Returns the UUID of the clan's leader.
     *
     * @return the leader's UUID, or empty if the clan has no leader (should not occur in practice)
     */
    public Optional<UUID> leaderId() {
        return members.stream()
                .filter(m -> m.role() == ClanRole.LEADER)
                .map(ClanMember::playerId)
                .findFirst();
    }

    /**
     * Checks whether the given player holds a permission, either directly or by
     * virtue of being the LEADER.
     *
     * @param playerUuid the player to test
     * @param permission the permission to check
     * @return {@code true} if the player is a member and holds the permission (or is LEADER)
     */
    public boolean hasPermission(UUID playerUuid, ClanPermission permission) {
        return getMember(playerUuid)
                .map(m -> m.hasPermission(permission))
                .orElse(false);
    }
}
