package net.sylphian.minecraft.clans.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Fired when a clan unclaims a chunk. Listen to {@link Pre} (cancellable, before the claim
 * is removed) or {@link Post} (after removal completes).
 */
public abstract class ClanUnclaimEvent extends Event {

    private final UUID clanId;
    private final String world;
    private final int chunkX;
    private final int chunkZ;

    protected ClanUnclaimEvent(UUID clanId, String world, int chunkX, int chunkZ) {
        this.clanId = clanId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** @return the clan that owned the chunk */
    public UUID getClanId() { return clanId; }

    /** @return the world containing the chunk */
    public String getWorld() { return world; }

    /** @return the chunk X coordinate */
    public int getChunkX() { return chunkX; }

    /** @return the chunk Z coordinate */
    public int getChunkZ() { return chunkZ; }

    /** Cancellable signal fired on the main thread before the claim is removed. */
    public static final class Pre extends ClanUnclaimEvent implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public Pre(UUID clanId, String world, int chunkX, int chunkZ) {
            super(clanId, world, chunkX, chunkZ);
        }

        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    /** Notification fired on the main thread after the claim is removed. */
    public static final class Post extends ClanUnclaimEvent {
        private static final HandlerList HANDLERS = new HandlerList();

        public Post(UUID clanId, String world, int chunkX, int chunkZ) {
            super(clanId, world, chunkX, chunkZ);
        }

        @Override public @NonNull HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
