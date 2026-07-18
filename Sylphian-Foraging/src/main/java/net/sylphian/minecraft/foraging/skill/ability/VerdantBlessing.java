package net.sylphian.minecraft.foraging.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.foraging.config.ForagingSkillConfig;
import net.sylphian.minecraft.gathering.registry.GatheringNodeService;
import net.sylphian.minecraft.gathering.world.LiveNode;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.ActivationResult;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active unlocked at level 25.
 *
 * <p>Instantly refreshes every depleted foraging node within range, bringing the
 * whole grove back at once with a burst of particles and sound at each.</p>
 */
public final class VerdantBlessing implements ActiveAbility {

    public static final String COOLDOWN_ID = "foraging:verdant-blessing";
    private static final String SKILL_ID = "foraging";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<ForagingSkillConfig> config;
    private final CooldownManager cooldowns;

    /**
     * @param config    supplier for the current config snapshot
     * @param cooldowns the shared cooldown manager
     */
    public VerdantBlessing(Supplier<ForagingSkillConfig> config, CooldownManager cooldowns) {
        this.config = config;
        this.cooldowns = cooldowns;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Verdant Blessing"; }
    @Override public String description() { return "Refresh every depleted foraging node around you at once."; }
    @Override public int    unlockLevel() { return 25; }

    @Override
    public ActivationResult onActivate(Player player, UUID uuid) {
        long remaining = cooldowns.getRemainingMillis(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Verdant Blessing: <white>" + (remaining / 1000) + "s remaining."));
            return ActivationResult.blocked();
        }

        ForagingSkillConfig cfg = config.get();
        List<LiveNode> depleted = GatheringNodeService.nearby(player.getLocation(), cfg.verdantBlessingRadius()).stream()
                .filter(node -> SKILL_ID.equals(node.type().skillId()))
                .filter(node -> node.state() == LiveNode.State.DEPLETED)
                .toList();

        if (depleted.isEmpty()) {
            player.sendActionBar(MINI.deserialize("<yellow>Verdant Blessing: <white>no depleted foraging nodes nearby."));
            return ActivationResult.blocked();
        }

        for (LiveNode node : depleted) {
            GatheringNodeService.refresh(node);
            Location centre = new Location(node.world(), node.x() + 0.5, node.y() + 0.5, node.z() + 0.5);
            node.world().spawnParticle(Particle.HAPPY_VILLAGER, centre, 15, 0.4, 0.4, 0.4, 0.0);
            node.world().playSound(centre, Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
        }

        cooldowns.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(cfg.verdantBlessingCooldownSeconds()));
        player.sendActionBar(MINI.deserialize(
                "<green>Verdant Blessing! <white>Restored " + depleted.size() + " node"
                + (depleted.size() == 1 ? "" : "s") + "."));
        return ActivationResult.used(
                "restored " + depleted.size() + " node" + (depleted.size() == 1 ? "" : "s"));
    }

    @Override
    public String selectionStatus(UUID uuid) {
        long s = cooldowns.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        return cooldowns.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
