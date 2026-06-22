package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired when a clan is disbanded. Listen to {@link Pre} (cancellable, before the clan and
 * its data are removed) or {@link Post} (after removal completes).
 */
public abstract class ClanDisbandEvent extends Event {

    private final UUID clanId;

    protected ClanDisbandEvent(UUID clanId) {
        this.clanId = clanId;
    }

    /** @return the UUID of the clan being disbanded */
    public UUID getClanId() { return clanId; }

    /** Cancellable signal fired on the main thread before the clan is removed. */
    public static final class Pre extends ClanDisbandEvent implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public Pre(UUID clanId) {
            super(clanId);
        }

        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    /** Notification fired on the main thread after the clan is removed. */
    public static final class Post extends ClanDisbandEvent {
        private static final HandlerList HANDLERS = new HandlerList();

        public Post(UUID clanId) {
            super(clanId);
        }

        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
