package net.sylphian.minecraft.foraging.skill;

import net.sylphian.minecraft.foraging.SylphianForaging;
import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.foraging.skill.ability.ForagersVigour;
import net.sylphian.minecraft.foraging.skill.ability.GentleTouch;
import net.sylphian.minecraft.foraging.skill.ability.Herbalist;
import net.sylphian.minecraft.foraging.skill.ability.Regrowth;
import net.sylphian.minecraft.foraging.skill.ability.VerdantBlessing;
import net.sylphian.minecraft.foraging.skill.ability.WildAbundance;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestTrigger;
import net.sylphian.minecraft.foraging.skill.trigger.ForageHarvestedTrigger;
import net.sylphian.minecraft.gathering.event.NodeHarvestEvent;
import net.sylphian.minecraft.gathering.event.NodeHarvestedEvent;
import net.sylphian.minecraft.gathering.registry.GatheringNodeService;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Foraging skill contributed to Sylphian-Skills. Coordinates the two
 * gathering harvest events into passive dispatch, and scopes ability activation
 * to real foraging nodes.
 */
public final class ForagingSkill extends AbstractSkill {

    private static final String SKILL_ID = "foraging";

    private final SylphianForaging plugin;
    private volatile ForagingSkillConfig config;

    private final Map<UUID, Map<String, Long>> recentVariety = new ConcurrentHashMap<>();

    public ForagingSkill(SylphianForaging plugin) {
        super(SKILL_ID, "Foraging");
        this.plugin = plugin;
    }

    @Override
    public void registerListeners(Plugin owningPlugin, SkillsAPI api) {
        this.config = ForagingSkillConfig.from(plugin.getConfig(), plugin.getLogger());

        addAbility(new GentleTouch(() -> config, uuid -> api.getCachedLevel(uuid, SKILL_ID)));
        addAbility(new Regrowth(() -> config, api.getCooldownManager(), api.getActiveBuffTracker(), owningPlugin));
        addAbility(new Herbalist(() -> config));
        addAbility(new ForagersVigour(() -> config, api.getCooldownManager(), api.getActiveBuffTracker(), owningPlugin));
        addAbility(new WildAbundance(() -> config, recentVariety));
        addAbility(new VerdantBlessing(() -> config, api.getCooldownManager()));

        super.registerListeners(owningPlugin, api);
    }

    @Override
    public void reload() {
        this.config = ForagingSkillConfig.from(plugin.getConfig(), plugin.getLogger());
    }

    /**
     * Scopes the sneak-right-click ability gesture to real foraging nodes only.
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

        ForageHarvestTrigger trigger = new ForageHarvestTrigger(event);
        firePassives(trigger, player, uuid);

        if (isWatched(uuid)) sendHarvestTrace(player, trigger, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNodeHarvested(NodeHarvestedEvent event) {
        if (!SKILL_ID.equals(event.node().type().skillId())) return;
        firePassives(new ForageHarvestedTrigger(event), event.player(), event.player().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        recentVariety.remove(uuid);
        unwatch(uuid);
    }

    private void sendHarvestTrace(Player player, ForageHarvestTrigger trigger, NodeHarvestEvent event) {
        UUID uuid = player.getUniqueId();
        sendTrace(uuid, TraceReport.of("<green>", "Harvest")
                .subject(player.getName())
                .level(skillsApi.getCachedLevel(uuid, SKILL_ID))
                .context("<white>" + event.node().type().id())
                .entries(trigger.traceEntries())
                .result("Yield", String.format("<white>x%.2f", event.getYieldMultiplier()))
                .result("XP", String.format("<white>x%.2f", event.getXpMultiplier())));
    }
}
