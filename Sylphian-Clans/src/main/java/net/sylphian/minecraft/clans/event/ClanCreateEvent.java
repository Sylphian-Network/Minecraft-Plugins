package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired when a clan is created. Listen to {@link Pre} (cancellable, before the clan is
 * written) or {@link Post} (after it is persisted).
 *
 * <p>Carries the clan UUID, the founder's UUID, and the chosen name. Listeners that need
 * the full clan should re-query via {@link net.sylphian.minecraft.clans.api.ClanProvider}.</p>
 */
public abstract class ClanCreateEvent extends Event {

    private final UUID clanId;
    private final UUID founderUuid;
    private final String name;

    protected ClanCreateEvent(UUID clanId, UUID founderUuid, String name) {
        this.clanId = clanId;
        this.founderUuid = founderUuid;
        this.name = name;
    }

    /** @return the UUID of the clan being created */
    public UUID getClanId() { return clanId; }

    /** @return the UUID of the founding player (the first leader) */
    public UUID getFounderUuid() { return founderUuid; }

    /** @return the chosen clan name */
    public String getName() { return name; }

    /** Cancellable signal fired on the main thread before the clan is written. */
    public static final class Pre extends ClanCreateEvent implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public Pre(UUID clanId, UUID founderUuid, String name) {
            super(clanId, founderUuid, name);
        }

        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    /** Notification fired on the main thread after the clan is persisted. */
    public static final class Post extends ClanCreateEvent {
        private static final HandlerList HANDLERS = new HandlerList();

        public Post(UUID clanId, UUID founderUuid, String name) {
            super(clanId, founderUuid, name);
        }

        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
