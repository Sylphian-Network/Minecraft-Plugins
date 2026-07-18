package net.sylphian.minecraft.gathering.event;

import net.sylphian.minecraft.gathering.world.LiveNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * Fired on the main thread at the end of a successful harvest for read-only
 * observation (streak trackers, stats). Not cancellable; handlers must not
 * mutate the harvest outcome.
 */
public class NodeHarvestedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final LiveNode node;
    private final Map<String, Integer> itemsGiven;
    private final long xpAwarded;

    public NodeHarvestedEvent(Player player, LiveNode node, Map<String, Integer> itemsGiven, long xpAwarded) {
        this.player = player;
        this.node = node;
        this.itemsGiven = itemsGiven;
        this.xpAwarded = xpAwarded;
    }

    /** @return the harvesting player */
    public Player player() {
        return player;
    }

    /** @return the harvested node */
    public LiveNode node() {
        return node;
    }

    /** @return item ids to the total amount granted on this harvest */
    public Map<String, Integer> itemsGiven() {
        return itemsGiven;
    }

    /** @return the XP awarded for this harvest */
    public long xpAwarded() {
        return xpAwarded;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
