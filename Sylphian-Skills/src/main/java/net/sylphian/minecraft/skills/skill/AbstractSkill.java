package net.sylphian.minecraft.skills.skill;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
public abstract class AbstractSkill implements Skill, Listener, Watchable {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final String id;
    private final String displayName;
    private final List<Ability> abilities = new ArrayList<>();

    /**
     * Available to subclass {@code @EventHandler} methods and helpers once
     * {@link #registerListeners} has been called.
     */
    protected SkillsAPI skillsApi;

    /** Maps watched player UUIDs to the admin {@link CommandSender} receiving their trace output. */
    private final Map<UUID, CommandSender> debugWatchers = new ConcurrentHashMap<>();

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
     * Dispatches a passive trigger to every {@link PassiveAbility} registered
     * with this skill that accepts it and whose unlock level the player meets.
     *
     * @param trigger the trigger token carrying event context and accumulating outputs
     * @param player  the player the event concerns
     * @param uuid    the player's UUID
     */
    protected void firePassives(PassiveTrigger trigger, Player player, UUID uuid) {
        int level = skillsApi.getCachedLevel(uuid, getId());
        for (Ability ability : abilities) {
            if (ability instanceof PassiveAbility passive
                    && passive.accepts(trigger)
                    && level >= passive.unlockLevel()) {
                passive.onPassiveTrigger(player, uuid, trigger);
            }
        }
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

    /**
     * Removes any debug watch session where the quitting player is the watcher.
     * Prevents trace messages being sent to a disconnected admin.
     */
    @EventHandler
    public void onWatcherQuit(PlayerQuitEvent event) {
        UUID quitterId = event.getPlayer().getUniqueId();
        debugWatchers.values().removeIf(
                watcher -> watcher instanceof Player p && p.getUniqueId().equals(quitterId));
    }

    @Override
    public void watch(UUID playerUuid, CommandSender watcher) {
        debugWatchers.put(playerUuid, watcher);
    }

    @Override
    public void unwatch(UUID playerUuid) {
        debugWatchers.remove(playerUuid);
    }

    @Override
    public boolean isWatched(UUID playerUuid) {
        return debugWatchers.containsKey(playerUuid);
    }

    @Override
    public @Nullable CommandSender getWatcher(UUID playerUuid) {
        return debugWatchers.get(playerUuid);
    }

    /**
     * Sends a trace header line followed by per-ability contribution lines to the
     * watching admin for this player. Does nothing if the player is not being watched.
     *
     * <p>Subclasses call this after firing passives, passing their own header string
     * and the trigger's {@link TraceEntry} list. Any event-specific footer lines
     * (e.g. final hook times, awarded XP) should be appended by the caller via
     * {@link #getWatcher}.</p>
     *
     * @param uuid    the target player's UUID
     * @param header  a MiniMessage string summarising the event
     * @param entries the ability contributions recorded on the trigger token
     */
    protected void sendTrace(UUID uuid, String header, List<TraceEntry> entries) {
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;
        watcher.sendMessage(MINI.deserialize(header));
        if (entries.isEmpty()) {
            watcher.sendMessage(MINI.deserialize("<gray>  (no abilities contributed)"));
        } else {
            for (TraceEntry entry : entries) {
                String line = entry.active()
                        ? "<gray>  <yellow>- [Active] <white>" + entry.source() + " <white>" + entry.description()
                        : "<gray>  <aqua>- [Passive] <aqua>" + entry.source() + " <white>" + entry.description();
                watcher.sendMessage(MINI.deserialize(line));
            }
        }
    }
}
