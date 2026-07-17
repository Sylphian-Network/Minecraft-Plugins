package net.sylphian.minecraft.gathering.world;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Returns depleted nodes to available once their respawn deadline passes,
 * re-rolling their modifier as they come back.
 */
public final class RespawnScheduler {

    private final Plugin plugin;
    private final NodeManager nodeManager;
    private final PriorityQueue<LiveNode> pending = new PriorityQueue<>(Comparator.comparingLong(LiveNode::respawnDeadline));

    private int wakeTaskId = -1;
    private long wakeAt = Long.MAX_VALUE;

    public RespawnScheduler(Plugin plugin, NodeManager nodeManager) {
        this.plugin = plugin;
        this.nodeManager = nodeManager;
    }

    /**
     * Enqueues a freshly depleted node to respawn at its {@code respawnDeadline},
     * bringing the wake forward if this deadline is the soonest pending.
     *
     * @param node the depleted node, with its respawn deadline already set
     */
    public void schedule(LiveNode node) {
        pending.add(node);
        scheduleWake();
    }

    /** Cancels the pending wake and drops all queued respawns. Call on reload and disable. */
    public void clear() {
        cancelWake();
        pending.clear();
    }

    // Schedules a one-shot wake at the head deadline, unless a sooner one is already pending.
    private void scheduleWake() {
        LiveNode head = pending.peek();
        if (head == null) return;

        long deadline = head.respawnDeadline();
        if (wakeTaskId != -1 && wakeAt <= deadline) return;

        cancelWake();
        long delayTicks = Math.max(1L, (deadline - System.currentTimeMillis()) / 50L);
        wakeAt = deadline;
        wakeTaskId = Bukkit.getScheduler().runTaskLater(plugin, this::wake, delayTicks).getTaskId();
    }

    private void wake() {
        wakeTaskId = -1;
        wakeAt = Long.MAX_VALUE;

        long now = System.currentTimeMillis();
        LiveNode head;
        while ((head = pending.peek()) != null && now >= head.respawnDeadline()) {
            pending.poll();
            respawn(head);
        }
        scheduleWake();
    }

    private void respawn(LiveNode node) {
        if (node.state() != LiveNode.State.DEPLETED) return;
        nodeManager.refresh(node);
    }

    private void cancelWake() {
        if (wakeTaskId != -1) {
            Bukkit.getScheduler().cancelTask(wakeTaskId);
            wakeTaskId = -1;
        }
        wakeAt = Long.MAX_VALUE;
    }
}
