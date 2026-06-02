package net.sylphian.minecraft.scoreboard.api;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Convenience base class for {@link SidebarContributor} implementations.
 *
 * <p>Handles the boilerplate of storing and returning the contributor's
 * ID and priority, and exposes a shared {@link MiniMessage} instance so
 * subclasses do not need to instantiate their own.</p>
 */
public abstract class AbstractSidebarContributor implements SidebarContributor {

    protected static final MiniMessage MINI = MiniMessage.miniMessage();

    private final String id;
    private final int priority;

    /**
     * @param id       the unique contributor identifier
     * @param priority the vertical position relative to other contributors; lower appears higher
     */
    protected AbstractSidebarContributor(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    @Override
    public String getId() { return id; }

    @Override
    public int getPriority() { return priority; }
}