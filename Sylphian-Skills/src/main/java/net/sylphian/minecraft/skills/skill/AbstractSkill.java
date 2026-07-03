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
     * Renders one trace block to the watching admin in the standard layout shared by every
     * skill: a header line, the per-ability contribution lines, then any result lines.
     * Does nothing if the player is not being watched.
     *
     * @param uuid   the target player's UUID
     * @param report the structured trace block to render
     */
    protected void sendTrace(UUID uuid, TraceReport report) {
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;

        StringBuilder header = new StringBuilder(report.color() + "- " + report.event() + " <white>" + report.subject());
        if (report.level() >= 0) {
            header.append(" <dark_gray>| <gray>Lv <white>").append(report.level());
        }
        if (report.context() != null && !report.context().isEmpty()) {
            header.append(" <dark_gray>| ").append(report.context());
        }
        watcher.sendMessage(MINI.deserialize(header.toString()));

        if (report.hasEntrySection()) {
            if (report.entries().isEmpty()) {
                watcher.sendMessage(MINI.deserialize("<gray>  <dark_gray>(no abilities contributed)"));
            } else {
                for (TraceEntry entry : report.entries()) {
                    watcher.sendMessage(MINI.deserialize(formatEntry(entry)));
                }
            }
        }

        for (TraceReport.Result result : report.results()) {
            watcher.sendMessage(MINI.deserialize(resultLine(result.label(), result.value())));
        }
    }

    /**
     * Emits a standard one-line {@code Active} trace block when the player is being watched.
     * Active abilities call this from {@code onActivate} so their use appears in the watch trace,
     * the way passive contributions appear as {@code [Passive]} lines.
     *
     * @param uuid    the activating player's UUID
     * @param subject the player name shown in the line
     * @param ability the ability's display name
     * @param detail  a short MiniMessage description of what happened
     */
    public void traceActiveUse(UUID uuid, String subject, String ability, String detail) {
        sendTrace(uuid, TraceReport.of("<yellow>", "Active")
                .subject(subject)
                .level(skillsApi.getCachedLevel(uuid, getId()))
                .context("<yellow>" + ability + " <dark_gray>| <gray>" + detail));
    }

    /**
     * Sends a single standalone result line in the standard trace layout. Use for results
     * that arrive in a later event than the block header (e.g. XP awarded after a roll).
     *
     * @param uuid  the target player's UUID
     * @param label the result label
     * @param value the MiniMessage value string
     */
    protected void sendTraceResult(UUID uuid, String label, String value) {
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;
        watcher.sendMessage(MINI.deserialize(resultLine(label, value)));
    }

    private static String formatEntry(TraceEntry entry) {
        return entry.active()
                ? "<gray>  <yellow>- [Active] <white>" + entry.source() + " <white>" + entry.description()
                : "<gray>  <aqua>- [Passive] <aqua>" + entry.source() + " <white>" + entry.description();
    }

    private static String resultLine(String label, String value) {
        return "<gray>  " + label + ": " + value;
    }
}
