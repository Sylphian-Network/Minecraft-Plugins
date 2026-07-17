package net.sylphian.minecraft.logging.skill;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.event.NodeHarvestedEvent;
import net.sylphian.minecraft.gathering.registry.GatheringNodeService;
import net.sylphian.minecraft.logging.SylphianLogging;
import net.sylphian.minecraft.logging.config.LoggingSkillConfig;
import net.sylphian.minecraft.logging.skill.ability.AncientTimber;
import net.sylphian.minecraft.logging.skill.ability.HeartwoodStrike;
import net.sylphian.minecraft.logging.skill.ability.SapTapper;
import net.sylphian.minecraft.logging.skill.ability.TimberFall;
import net.sylphian.minecraft.logging.skill.ability.WoodcuttersFrenzy;
import net.sylphian.minecraft.logging.skill.ability.WoodsmansRhythm;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestTrigger;
import net.sylphian.minecraft.logging.skill.trigger.LogHarvestedTrigger;
import net.sylphian.minecraft.skills.api.SkillsAPI;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import net.sylphian.minecraft.skills.skill.TraceReport;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Logging skill contributed to Sylphian-Skills. Coordinates the two gathering
 * harvest events into passive dispatch and pending-active application, and scopes
 * ability activation to real logging nodes.
 */
public final class LoggingSkill extends AbstractSkill {

    private static final String SKILL_ID = "logging";

    private final SylphianLogging plugin;
    private volatile LoggingSkillConfig config;

    private final Set<UUID> heartwoodPending = ConcurrentHashMap.newKeySet();
    private final Set<UUID> ancientTimberPending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, LoggingStreak> streaks = new ConcurrentHashMap<>();

    private HeartwoodStrike heartwoodStrike;
    private AncientTimber ancientTimber;

    public LoggingSkill(SylphianLogging plugin) {
        super(SKILL_ID, "Logging");
        this.plugin = plugin;
    }

    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = LoggingSkillConfig.from(plugin.getConfig(), plugin.getLogger());

        heartwoodStrike = new HeartwoodStrike(() -> config, api.getCooldownManager(), heartwoodPending);
        ancientTimber = new AncientTimber(() -> config, api.getCooldownManager(), ancientTimberPending);

        addAbility(new TimberFall(() -> config, uuid -> api.getCachedLevel(uuid, SKILL_ID)));
        addAbility(heartwoodStrike);
        addAbility(new SapTapper(() -> config));
        addAbility(new WoodcuttersFrenzy(() -> config, api.getCooldownManager(), api.getActiveBuffTracker(), owningPlugin));
        addAbility(new WoodsmansRhythm(() -> config, streaks));
        addAbility(ancientTimber);

        super.registerListeners(owningPlugin, api);
    }

    @Override
    public void reload() {
        this.config = LoggingSkillConfig.from(plugin.getConfig(), plugin.getLogger());
    }

    /**
     * Scopes the sneak-right-click ability gesture to real logging nodes only.
     */
    @Override
    public boolean isActivationTarget(Block block) {
        return GatheringNodeService.isNodeFor(block, SKILL_ID);
    }

    @EventHandler(ignoreCancelled = true)
    public void onNodeHarvest(NodeHarvestEvent event) {
        if (!SKILL_ID.equals(event.node().type().skillId())) return;
        Player player = event.player();
        UUID uuid = player.getUniqueId();

        LogHarvestTrigger trigger = new LogHarvestTrigger(event);
        firePassives(trigger, player, uuid);
        heartwoodStrike.applyOnHarvest(uuid, trigger);
        ancientTimber.applyOnHarvest(uuid, trigger);

        if (isWatched(uuid)) sendHarvestTrace(player, trigger, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNodeHarvested(NodeHarvestedEvent event) {
        if (!SKILL_ID.equals(event.node().type().skillId())) return;
        firePassives(new LogHarvestedTrigger(event), event.player(), event.player().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        heartwoodPending.remove(uuid);
        ancientTimberPending.remove(uuid);
        streaks.remove(uuid);
        unwatch(uuid);
    }

    private void sendHarvestTrace(Player player, LogHarvestTrigger trigger, NodeHarvestEvent event) {
        UUID uuid = player.getUniqueId();
        sendTrace(uuid, TraceReport.of("<gold>", "Harvest")
                .subject(player.getName())
                .level(skillsApi.getCachedLevel(uuid, SKILL_ID))
                .context("<white>" + event.node().type().id())
                .entries(trigger.traceEntries())
                .result("Yield", String.format("<white>x%.2f", event.getYieldMultiplier()))
                .result("XP", String.format("<white>x%.2f", event.getXpMultiplier())));
    }
}
