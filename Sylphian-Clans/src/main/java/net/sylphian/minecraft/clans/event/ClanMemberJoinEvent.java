package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired when a player joins a clan. Listen to {@link Pre} (cancellable, before the member
 * is written) or {@link Post} (after the member is persisted).
 */
public abstract class ClanMemberJoinEvent extends Event {

    private final UUID clanId;
    private final UUID playerUuid;

    protected ClanMemberJoinEvent(UUID clanId, UUID playerUuid) {
        this.clanId = clanId;
        this.playerUuid = playerUuid;
    }

    /** @return the UUID of the clan being joined */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the joining player */
    public UUID getPlayerUuid() { return playerUuid; }

    /** Cancellable signal fired on the main thread before the member is written. */
    public static final class Pre extends ClanMemberJoinEvent implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public Pre(UUID clanId, UUID playerUuid) {
            super(clanId, playerUuid);
        }

        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    /** Notification fired on the main thread after the member is persisted. */
    public static final class Post extends ClanMemberJoinEvent {
        private static final HandlerList HANDLERS = new HandlerList();

        public Post(UUID clanId, UUID playerUuid) {
            super(clanId, playerUuid);
        }

        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
