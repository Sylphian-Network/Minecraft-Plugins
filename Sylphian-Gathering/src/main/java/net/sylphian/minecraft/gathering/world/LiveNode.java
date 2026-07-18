package net.sylphian.minecraft.gathering.world;

import net.sylphian.minecraft.gathering.node.NodeModifier;
import net.sylphian.minecraft.gathering.node.NodeType;
import org.bukkit.Material;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

/**
 * The mutable, in-memory state of one placed node. Never persisted: nodes are
 * re-derived from config on startup and reset to available on restart.
 */
public final class LiveNode {

    /** Whether the node can currently be harvested. */
    public enum State {
        AVAILABLE,
        DEPLETED
    }

    private final NodeType type;
    private final World world;
    private final int x;
    private final int y;
    private final int z;

    private State state = State.AVAILABLE;
    private @Nullable NodeModifier activeModifier;
    private long respawnDeadline;

    public LiveNode(NodeType type, World world, int x, int y, int z) {
        this.type = type;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public NodeType type() {
        return type;
    }

    public World world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public @Nullable NodeModifier activeModifier() {
        return activeModifier;
    }

    public void setActiveModifier(@Nullable NodeModifier activeModifier) {
        this.activeModifier = activeModifier;
    }

    public long respawnDeadline() {
        return respawnDeadline;
    }

    public void setRespawnDeadline(long respawnDeadline) {
        this.respawnDeadline = respawnDeadline;
    }

    /**
     * @return the block this node should show for its current state and modifier
     */
    public Material currentBlock() {
        if (state == State.DEPLETED) return type.depletedBlock();
        if (activeModifier != null && activeModifier.blockOverride() != null) {
            return activeModifier.blockOverride();
        }
        return type.availableBlock();
    }
}
