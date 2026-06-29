package net.sylphian.minecraft.cooking.skill;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.SylphianCooking;
import net.sylphian.minecraft.cooking.event.CookingCompleteEvent;
import net.sylphian.minecraft.cooking.event.CookingStartEvent;
import net.sylphian.minecraft.cooking.event.CookingXpEvent;
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
 * The Cooking skill. Handles {@link CookingStartEvent} and {@link CookingCompleteEvent}
 * to fire passives, and {@link CookingXpEvent} to award XP.
 * All event handlers run on the main thread.
 */
public final class CookingSkill extends AbstractSkill {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SylphianCooking plugin;
    private volatile CookingSkillConfig config;

    public CookingSkill(SylphianCooking plugin) {
        super("cooking", "Cooking");
        this.plugin = plugin;
    }

    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = CookingSkillConfig.from(plugin.getConfig());
        super.registerListeners(owningPlugin, api);
    }

    @Override
    public void reload() {
        this.config = CookingSkillConfig.from(plugin.getConfig());
    }

    /** Fires passive start triggers and applies any accumulated cook-time reduction to the event. */
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
     * Fires passive complete triggers and writes quality shifts, XP multiplier, and
     * any bonus output back to the event. Quality rolling happens in the service after
     * this handler returns.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingComplete(CookingCompleteEvent event) {
        UUID uuid = event.getLastInteractor();
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;

        CookingCompleteTrigger trigger = new CookingCompleteTrigger(event.getRecipe());
        firePassives(trigger, player, uuid);

        trigger.qualityShifts().forEach(event::addQualityShift);
        event.multiplyXp(trigger.xpMultiplier());

        if (trigger.bonusOutput() != null) {
            event.setBonusOutput(trigger.bonusOutput());
        }

        if (isWatched(uuid)) sendCookingCompleteTrace(player, trigger);
    }

    /**
     * Awards XP once the service has rolled quality and fired {@link CookingXpEvent}.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingXp(CookingXpEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayerUuid());
        if (player == null) return;

        CookingSkillConfig cfg = config;
        long baseXp  = cfg.xpPerRecipe();
        long finalXp = Math.max(1L, (long) (baseXp * event.getQuality().xpMultiplier() * event.getXpMultiplier()));
        skillsApi.awardXP(player, "cooking", finalXp);

        if (isWatched(event.getPlayerUuid())) sendXpTrace(player, event, baseXp, finalXp);
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

    private void sendCookingCompleteTrace(Player player, CookingCompleteTrigger trigger) {
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
                "<gray>  Shifts: <white>" + trigger.qualityShifts()
                + (trigger.bonusOutput() != null ? " <dark_gray>| <yellow>bonus drop queued" : "")));
    }

    private void sendXpTrace(Player player, CookingXpEvent event, long baseXp, long finalXp) {
        UUID uuid = player.getUniqueId();
        CommandSender watcher = getWatcher(uuid);
        if (watcher == null) return;
        watcher.sendMessage(MINI.deserialize(
                "<gray>  Quality: <white>" + event.getQuality().name()
                + " <dark_gray>| <gray>XP: <white>" + baseXp
                + " <gray>x" + String.format("%.1f", event.getQuality().xpMultiplier())
                + " <gray>x" + String.format("%.2f", event.getXpMultiplier())
                + " <gray>= <white>" + finalXp));
    }
}
