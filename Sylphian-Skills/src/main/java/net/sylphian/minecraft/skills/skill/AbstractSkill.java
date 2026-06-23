package net.sylphian.minecraft.skills.skill;

import net.sylphian.minecraft.skills.api.SkillsAPI;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link Skill} providing id, display name, ability
 * registration, and default listener registration.
 *
 * <p>Subclasses implement their event logic as {@code @EventHandler} methods
 * directly on the class, and register abilities via {@link #addAbility} in
 * their constructor. Override {@link #registerListeners} to perform one-time
 * setup (e.g. reading a config snapshot) before calling
 * {@code super.registerListeners(owningPlugin, api)}, which stores
 * {@link #skillsApi} and registers {@code this} as a Bukkit listener.</p>
 *
 * <pre>
 *     {@literal @}Override
 *     public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
 *         this.config = MyConfig.from(plugin.getConfig());
 *         super.registerListeners(owningPlugin, api);
 *     }
 * </pre>
 */
public abstract class AbstractSkill implements Skill, Listener {

    private final String id;
    private final String displayName;
    private final List<Ability> abilities = new ArrayList<>();

    /**
     * Available to subclass {@code @EventHandler} methods and helpers once
     * {@link #registerListeners} has been called.
     */
    protected SkillsAPI skillsApi;

    /**
     * @param id          unique lowercase identifier, e.g. {@code "mining"}
     * @param displayName player-facing name, e.g. {@code "Mining"}
     */
    protected AbstractSkill(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Registers an ability with this skill. Call from the subclass constructor
     * for each ability the skill unlocks, in ascending level order.
     *
     * @param ability the ability to register
     */
    protected void addAbility(Ability ability) {
        abilities.add(ability);
    }

    /**
     * @return an unmodifiable view of this skill's abilities, in registration order
     */
    public List<Ability> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /**
     * Stores the skills API reference and registers {@code this} as a Bukkit
     * listener under {@code owningPlugin}.
     *
     * @param owningPlugin the plugin that owns this skill's listeners
     * @param api          the skills API for XP, cooldowns, and buff tracking
     */
    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.skillsApi = api;
        owningPlugin.getServer().getPluginManager().registerEvents(this, owningPlugin);
    }
}
