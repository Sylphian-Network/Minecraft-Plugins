package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Fired on the main thread after a clan's message of the day is set or cleared.
 * The MOTD has already been sanitised of interactivity before this event fires.
 */
public class ClanMotdChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID clanId;
    private final @Nullable String motd;

    /**
     * @param clanId the clan whose MOTD changed
     * @param motd   the new MOTD, or null if it was cleared
     */
    public ClanMotdChangeEvent(UUID clanId, @Nullable String motd) {
        this.clanId = clanId;
        this.motd = motd;
    }

    /** @return the clan whose MOTD changed */
    public UUID getClanId() { return clanId; }

    /** @return the new MOTD, or null if it was cleared */
    public @Nullable String getMotd() { return motd; }

    @Override
    public @NonNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
