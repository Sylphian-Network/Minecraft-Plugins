package net.sylphian.minecraft.scoreboard.api;

/**
 * Convenience base class for {@link SidebarContributor} implementations.
 *
 * <p>Handles the boilerplate of storing and returning the contributor's
 * ID and priority. Line construction should use {@link SidebarLine#of(String)}
 * rather than a locally held MiniMessage instance.</p>
 */
public abstract class AbstractSidebarContributor implements SidebarContributor {

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