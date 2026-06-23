package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired on the main thread after clan leadership passes from one member to another.
 */
public class ClanLeadershipTransferEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final UUID previousLeaderUuid;
    private final UUID newLeaderUuid;

    /**
     * @param clanId             the clan whose leadership changed
     * @param previousLeaderUuid the outgoing leader
     * @param newLeaderUuid      the incoming leader
     */
    public ClanLeadershipTransferEvent(UUID clanId, UUID previousLeaderUuid, UUID newLeaderUuid) {
        this.clanId = clanId;
        this.previousLeaderUuid = previousLeaderUuid;
        this.newLeaderUuid = newLeaderUuid;
    }

    /** @return the clan whose leadership changed */
    public UUID getClanId() { return clanId; }

    /** @return the outgoing leader */
    public UUID getPreviousLeaderUuid() { return previousLeaderUuid; }

    /** @return the incoming leader */
    public UUID getNewLeaderUuid() { return newLeaderUuid; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
