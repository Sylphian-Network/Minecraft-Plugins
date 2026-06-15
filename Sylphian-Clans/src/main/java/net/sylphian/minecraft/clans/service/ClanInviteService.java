package net.sylphian.minecraft.clans.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages pending clan invites in memory. Invites are never persisted to the
 * database; they expire after a configurable duration and are lost on restart.
 *
 * <p>A player may hold multiple pending invites from different clans simultaneously.</p>
 */
public class ClanInviteService {

    private final long expirySeconds;

    /** Maps each invited player's UUID to their list of pending invites. */
    private final Map<UUID, CopyOnWriteArrayList<PendingInvite>> pending = new ConcurrentHashMap<>();

    /**
     * @param expirySeconds how long an invite remains valid after it is sent
     */
    public ClanInviteService(long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    /**
     * Creates a pending invite for the given player from the given clan.
     * Any existing expired invites for this player are pruned first.
     * A player may not receive a second invite from the same clan while one is active.
     *
     * @param clanId     the inviting clan's UUID
     * @param clanName   the inviting clan's display name, stored for accept/decline lookup
     * @param inviterUuid the UUID of the member who sent the invite
     * @param inviteeUuid the UUID of the player being invited
     * @return {@code false} if an active invite from this clan already exists
     */
    public boolean addInvite(UUID clanId, String clanName, UUID inviterUuid, UUID inviteeUuid) {
        CopyOnWriteArrayList<PendingInvite> invites = pending.computeIfAbsent(inviteeUuid, k -> new CopyOnWriteArrayList<>());
        pruneExpired(invites);

        boolean alreadyInvited = invites.stream().anyMatch(i -> i.clanId().equals(clanId));
        if (alreadyInvited) return false;

        invites.add(new PendingInvite(clanId, clanName, inviterUuid, Instant.now().plusSeconds(expirySeconds)));
        return true;
    }

    /**
     * Returns all non-expired pending invites for a player.
     *
     * @param inviteeUuid the invited player's UUID
     * @return a snapshot list of active invites; empty if none exist
     */
    public List<PendingInvite> getPendingInvites(UUID inviteeUuid) {
        CopyOnWriteArrayList<PendingInvite> invites = pending.get(inviteeUuid);
        if (invites == null) return List.of();
        pruneExpired(invites);
        return List.copyOf(invites);
    }

    /**
     * Finds and removes an active invite by clan name (case-insensitive).
     * Returns empty if the invite does not exist or has expired.
     *
     * @param inviteeUuid the invited player's UUID
     * @param clanName    the clan name to accept or decline
     * @return the matching invite, or empty
     */
    public Optional<PendingInvite> consumeInvite(UUID inviteeUuid, String clanName) {
        List<PendingInvite> invites = pending.getOrDefault(inviteeUuid, null);
        if (invites == null) return Optional.empty();

        pruneExpired(invites);

        Optional<PendingInvite> found = invites.stream()
                .filter(i -> i.clanName().equalsIgnoreCase(clanName))
                .findFirst();

        found.ifPresent(invites::remove);
        if (invites.isEmpty()) pending.remove(inviteeUuid);

        return found;
    }

    /** Removes all invites for a player. Called when a player joins a clan. */
    public void clearInvites(UUID inviteeUuid) {
        pending.remove(inviteeUuid);
    }

    private void pruneExpired(List<PendingInvite> invites) {
        invites.removeIf(i -> i.expiresAt().isBefore(Instant.now()));
    }

    /**
     * A single pending clan invite.
     *
     * @param clanId      the inviting clan's UUID
     * @param clanName    the inviting clan's display name
     * @param inviterUuid the UUID of the member who sent the invite
     * @param expiresAt   when this invite expires
     */
    public record PendingInvite(UUID clanId, String clanName, UUID inviterUuid, Instant expiresAt) {}
}
