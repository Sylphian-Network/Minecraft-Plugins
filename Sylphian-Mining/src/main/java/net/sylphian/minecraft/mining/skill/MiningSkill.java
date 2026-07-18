package net.sylphian.minecraft.mining.skill;

import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.event.NodeHarvestedEvent;
import net.sylphian.minecraft.gathering.registry.GatheringNodeService;
import net.sylphian.minecraft.mining.SylphianMining;
import net.sylphian.minecraft.mining.config.MiningSkillConfig;
import net.sylphian.minecraft.mining.skill.ability.Mineralogist;
import net.sylphian.minecraft.mining.skill.ability.Motherlode;
import net.sylphian.minecraft.mining.skill.ability.OreSense;
import net.sylphian.minecraft.mining.skill.ability.ProspectorsEye;
import net.sylphian.minecraft.mining.skill.ability.SteadyRhythm;
import net.sylphian.minecraft.mining.skill.ability.VeinSurge;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestTrigger;
import net.sylphian.minecraft.mining.skill.trigger.OreHarvestedTrigger;
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
 * The Mining skill contributed to Sylphian-Skills. Coordinates the two gathering
 * harvest events into passive dispatch and pending-active application, and scopes
 * ability activation to real mining nodes.
 */
public final class MiningSkill extends AbstractSkill {

    private static final String SKILL_ID = "mining";

    private final SylphianMining plugin;
    private volatile MiningSkillConfig config;

    private final Set<UUID> veinSurgePending = ConcurrentHashMap.newKeySet();
    private final Set<UUID> motherlodePending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, MiningStreak> streaks = new ConcurrentHashMap<>();

    private VeinSurge veinSurge;
    private Motherlode motherlode;

    public MiningSkill(SylphianMining plugin) {
        super(SKILL_ID, "Mining");
        this.plugin = plugin;
    }

    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = MiningSkillConfig.from(plugin.getConfig(), plugin.getLogger());

        veinSurge = new VeinSurge(() -> config, api.getCooldownManager(), veinSurgePending);
        motherlode = new Motherlode(() -> config, api.getCooldownManager(), motherlodePending);

        addAbility(new OreSense(() -> config, uuid -> api.getCachedLevel(uuid, SKILL_ID)));
        addAbility(veinSurge);
        addAbility(new Mineralogist(() -> config));
        addAbility(new ProspectorsEye(() -> config, api.getCooldownManager(), api.getActiveBuffTracker(), owningPlugin));
        addAbility(new SteadyRhythm(() -> config, streaks));
        addAbility(motherlode);

        super.registerListeners(owningPlugin, api);
    }

    @Override
    public void reload() {
        this.config = MiningSkillConfig.from(plugin.getConfig(), plugin.getLogger());
    }

    /**
     * Scopes the sneak-right-click ability gesture to real mining nodes only, so
     * clicking a plain block of the same material never opens the menu.
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

        OreHarvestTrigger trigger = new OreHarvestTrigger(event);
        firePassives(trigger, player, uuid);
        veinSurge.applyOnHarvest(uuid, trigger);
        motherlode.applyOnHarvest(uuid, trigger);

        if (isWatched(uuid)) sendHarvestTrace(player, trigger, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNodeHarvested(NodeHarvestedEvent event) {
        if (!SKILL_ID.equals(event.node().type().skillId())) return;
        firePassives(new OreHarvestedTrigger(event), event.player(), event.player().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        veinSurgePending.remove(uuid);
        motherlodePending.remove(uuid);
        streaks.remove(uuid);
        unwatch(uuid);
    }

    private void sendHarvestTrace(Player player, OreHarvestTrigger trigger, NodeHarvestEvent event) {
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
