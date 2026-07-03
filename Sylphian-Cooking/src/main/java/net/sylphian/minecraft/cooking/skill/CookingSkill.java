package net.sylphian.minecraft.cooking.skill;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.SylphianCooking;
import net.sylphian.minecraft.cooking.event.CookingCompleteEvent;
import net.sylphian.minecraft.cooking.event.CookingDiscoveryEvent;
import net.sylphian.minecraft.cooking.event.CookingMasteryMilestoneEvent;
import net.sylphian.minecraft.cooking.event.CookingStartEvent;
import net.sylphian.minecraft.cooking.event.CookingXpEvent;
import net.sylphian.minecraft.cooking.listener.CookingStationListener;
import net.sylphian.minecraft.cooking.skill.ability.Banquet;
import net.sylphian.minecraft.cooking.skill.ability.CookStreak;
import net.sylphian.minecraft.cooking.skill.ability.EfficientCook;
import net.sylphian.minecraft.cooking.skill.ability.PerfectSear;
import net.sylphian.minecraft.cooking.skill.ability.QuickPrep;
import net.sylphian.minecraft.cooking.skill.ability.SeasonedHands;
import net.sylphian.minecraft.cooking.skill.ability.SecondWind;
import net.sylphian.minecraft.cooking.skill.trigger.CookingCompleteTrigger;
import net.sylphian.minecraft.cooking.skill.trigger.CookingStartTrigger;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import net.sylphian.minecraft.skills.skill.TraceReport;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    /** Seasoned Hands streak state, keyed by player UUID. Main-thread access only. */
    private final Map<UUID, CookStreak> streaks = new HashMap<>();

    public CookingSkill(SylphianCooking plugin) {
        super("cooking", "Cooking");
        this.plugin = plugin;
    }

    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = CookingSkillConfig.from(plugin.getConfig());

        var service = plugin.getStationService();

        addAbility(new Banquet(() -> config, api.getCooldownManager(), this));
        addAbility(new EfficientCook(() -> config));
        addAbility(new SecondWind(() -> config, api.getCooldownManager(), service, this));
        addAbility(new SeasonedHands(() -> config, streaks));
        addAbility(new PerfectSear(() -> config, api.getCooldownManager(), service, this));
        addAbility(new QuickPrep(() -> config));

        super.registerListeners(owningPlugin, api);
    }

    /** Sneak-right-clicking any cooking station block opens the active-ability menu. */
    @Override
    public Set<Material> activationBlocks() {
        return CookingStationListener.STATION_BLOCKS;
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
        if (trigger.shouldPreserveIngredient()) {
            event.setPreserveIngredient(true);
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

    /** Awards the one-time discovery bonus the first time a player cooks a recipe. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingDiscovery(CookingDiscoveryEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayerUuid());
        if (player == null) return;

        long xp = config.discoveryXp();
        if (xp > 0) skillsApi.awardXP(player, "cooking", xp);

        player.sendMessage(MINI.deserialize(
                "<gold>New recipe discovered! <gray>(<white>" + event.getRecipe().id()
                + "<gray>) <yellow>+" + xp + " XP"));
    }

    /** Awards the milestone bonus when a player reaches a configured mastery milestone. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCookingMilestone(CookingMasteryMilestoneEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayerUuid());
        if (player == null) return;

        long xp = config.milestoneXp();
        if (xp > 0) skillsApi.awardXP(player, "cooking", xp);

        player.sendMessage(MINI.deserialize(
                "<gold>Mastery milestone! <gray>(<white>" + event.getRecipe().id()
                + " <gray>x<white>" + event.getMilestone() + "<gray>) <yellow>+" + xp + " XP"));
    }

    /** Clears the player's streak state and any debug watch session where they are the subject. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        streaks.remove(uuid);
        unwatch(uuid);
    }

    private void sendCookingStartTrace(Player player, CookingStartTrigger trigger, CookingStartEvent event) {
        UUID uuid = player.getUniqueId();
        int level = skillsApi.getCachedLevel(uuid, "cooking");
        double combined = Math.min(CookingStartTrigger.MAX_REDUCTION, trigger.totalReduction());
        sendTrace(uuid, TraceReport.of("<gold>", "Cook Start")
                .subject(player.getName())
                .level(level)
                .context("<white>" + trigger.recipe().id())
                .entries(trigger.traceEntries())
                .result("Result", "<white>" + event.getEffectiveCookTime()
                        + "<gray>t <dark_gray>(<gray>combined <white>"
                        + String.format("%.0f%%", combined * 100) + "<gray>)"));
    }

    private void sendCookingCompleteTrace(Player player, CookingCompleteTrigger trigger) {
        UUID uuid = player.getUniqueId();
        int level = skillsApi.getCachedLevel(uuid, "cooking");
        sendTrace(uuid, TraceReport.of("<green>", "Cook Complete")
                .subject(player.getName())
                .level(level)
                .context("<white>" + trigger.recipe().id())
                .entries(trigger.traceEntries()));
    }

    private void sendXpTrace(Player player, CookingXpEvent event, long baseXp, long finalXp) {
        UUID uuid = player.getUniqueId();
        sendTraceResult(uuid, "Quality", "<white>" + event.getQuality().name());
        sendTraceResult(uuid, "XP", "<white>" + baseXp + " <gray>base"
                + " <gray>x" + String.format("%.1f", event.getQuality().xpMultiplier())
                + " <gray>x" + String.format("%.2f", event.getXpMultiplier())
                + " <gray>-> <white>" + finalXp + " <gray>awarded");
    }
}
