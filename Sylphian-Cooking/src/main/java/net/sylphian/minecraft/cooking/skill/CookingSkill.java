package net.sylphian.minecraft.cooking.skill;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.SylphianCooking;
import net.sylphian.minecraft.cooking.event.CookingCompleteEvent;
import net.sylphian.minecraft.cooking.event.CookingStartEvent;
import net.sylphian.minecraft.cooking.skill.trigger.CookingCompleteTrigger;
import net.sylphian.minecraft.cooking.skill.trigger.CookingStartTrigger;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * The Cooking skill contributed to Sylphian-Skills by Sylphian-Cooking.
 *
 * <p>Acts as the event coordinator: listens to {@link CookingStartEvent} and
 * {@link CookingCompleteEvent} fired by {@link net.sylphian.minecraft.cooking.station.CookingStationService},
 * routes them through the passive ability framework, awards XP on completion,
 * and propagates bonus output back to the event for the service to drop.</p>
 *
 * <p>Active ability selection (sneak + right-click with a held item) is handled
 * generically by {@code ActiveAbilityCoordinator} in Sylphian-Skills. This skill
 * does not override {@link #activationMaterial()} because cooking abilities are
 * triggered from within the station GUI rather than through an item gesture.</p>
 *
 * <p>All state maps and event handlers are accessed on the main thread only.</p>
 */
public final class CookingSkill extends AbstractSkill {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianCooking plugin;
    private volatile CookingSkillConfig config;

    /**
     * @param plugin the owning Cooking plugin instance
     */
    public CookingSkill(SylphianCooking plugin) {
        super("cooking", "Cooking");
        this.plugin = plugin;
    }

    /**
     * Constructs abilities with their injected dependencies, registers them in
     * unlock-level order, reads the initial config snapshot, and registers this
     * skill as a Bukkit listener via the parent.
     */
    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = CookingSkillConfig.from(plugin.getConfig());

        super.registerListeners(owningPlugin, api);
    }

    /**
     * Swaps the config snapshot on a Cooking plugin reload.
     */
    @Override
    public void reload() {
        this.config = CookingSkillConfig.from(plugin.getConfig());
    }

    /**
     * Handles the start of a new cook cycle. Fires passive start triggers to accumulate
     * cook-time reductions, then applies the combined reduction to the event so the
     * service uses the modified cook time for this cycle.
     *
     * <p>Runs at {@link EventPriority#HIGH} so it applies reductions before any other
     * plugin might observe the effective cook time.</p>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingStart(CookingStartEvent event) {
        UUID uuid = event.getLastInteractor();
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;

        CookingStartTrigger trigger = new CookingStartTrigger(event.getRecipe());
        firePassives(trigger, player, uuid);

        if (trigger.totalReduction() > 0) {
            double capped = Math.min(CookingStartTrigger.MAX_REDUCTION, trigger.totalReduction());
            int reduced = (int) (event.getEffectiveCookTime() * (1.0 - capped));
            event.setEffectiveCookTime(Math.max(20, reduced));
        }

        if (isWatched(uuid)) sendCookingStartTrace(player, trigger, event);
    }

    /**
     * Handles the completion of a cook cycle. Fires passive complete triggers to
     * accumulate XP multipliers and bonus output, awards XP to the last interactor,
     * and propagates any bonus output back to the event for the service to drop.
     *
     * <p>Runs at {@link EventPriority#HIGH}.</p>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingComplete(CookingCompleteEvent event) {
        UUID uuid = event.getLastInteractor();
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;

        CookingCompleteTrigger trigger = new CookingCompleteTrigger(event.getRecipe());
        firePassives(trigger, player, uuid);

        long baseXp  = config.xpPerRecipe();
        long finalXp = Math.max(1L, (long) (baseXp * trigger.xpMultiplier()));
        skillsApi.awardXP(player, "cooking", finalXp);

        if (trigger.bonusOutput() != null) {
            event.setBonusOutput(trigger.bonusOutput());
        }

        if (isWatched(uuid)) sendCookingCompleteTrace(player, trigger, baseXp, finalXp);
    }

    /** Clears any debug watch session where the quitting player is the subject. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unwatch(event.getPlayer().getUniqueId());
    }

    private void sendCookingStartTrace(Player player, CookingStartTrigger trigger, CookingStartEvent event) {
        UUID uuid = player.getUniqueId();
        int level = skillsApi.getCachedLevel(uuid, "cooking");
        sendTrace(uuid,
                "<gold>- Cook Start <white>" + player.getName()
                + " <dark_gray>| <gray>Lv <white>" + level
                + " <dark_gray>| <white>" + trigger.recipe().id(),
                trigger.traceEntries());
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;
        double combined = Math.min(CookingStartTrigger.MAX_REDUCTION, trigger.totalReduction());
        watcher.sendMessage(MINI.deserialize(
                "<gray>  Result: <white>" + event.getEffectiveCookTime()
                + "<gray>t <dark_gray>(<gray>combined <white>"
                + String.format("%.0f%%", combined * 100) + "<gray> reduction)"));
    }

    private void sendCookingCompleteTrace(Player player, CookingCompleteTrigger trigger,
                                          long baseXp, long finalXp) {
        UUID uuid = player.getUniqueId();
        int level = skillsApi.getCachedLevel(uuid, "cooking");
        sendTrace(uuid,
                "<green>- Cook Complete <white>" + player.getName()
                + " <dark_gray>| <gray>Lv <white>" + level
                + " <dark_gray>| <white>" + trigger.recipe().id(),
                trigger.traceEntries());
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;
        watcher.sendMessage(MINI.deserialize(
                "<gray>  XP: <white>" + baseXp + " <gray>base -> <white>" + finalXp + " <gray>awarded"
                + (trigger.bonusOutput() != null ? " <dark_gray>| <yellow>bonus yield dropped" : "")));
    }
}
