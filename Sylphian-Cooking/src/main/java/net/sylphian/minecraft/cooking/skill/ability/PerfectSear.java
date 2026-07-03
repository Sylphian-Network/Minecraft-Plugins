package net.sylphian.minecraft.cooking.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.skill.CookingSkillConfig;
import net.sylphian.minecraft.cooking.station.CookingStationService;
import net.sylphian.minecraft.skills.service.CooldownManager;
import net.sylphian.minecraft.skills.skill.AbstractSkill;
import net.sylphian.minecraft.skills.skill.ActiveAbility;
import net.sylphian.minecraft.skills.skill.StatusLevel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Active ability unlocked at level 25. Primes the targeted station so its next quality cook
 * rolls Perfect, skipping the usual quality RNG.
 */
public final class PerfectSear implements ActiveAbility {

    /** Cooldown key used with {@link CooldownManager}. */
    public static final String COOLDOWN_ID = "cooking:perfect-sear";

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<CookingSkillConfig> config;
    private final CooldownManager cooldownManager;
    private final CookingStationService service;
    private final AbstractSkill skill;

    /**
     * @param config          supplier for the current config snapshot
     * @param cooldownManager the shared cooldown manager
     * @param service         the cooking station service
     * @param skill           the owning skill, used to emit watch-trace lines
     */
    public PerfectSear(Supplier<CookingSkillConfig> config, CooldownManager cooldownManager,
                       CookingStationService service, AbstractSkill skill) {
        this.config = config;
        this.cooldownManager = cooldownManager;
        this.service = service;
        this.skill = skill;
    }

    @Override public String id()          { return COOLDOWN_ID; }
    @Override public String name()        { return "Perfect Sear"; }
    @Override public String description() { return "The next dish cooked at this station is Perfect."; }
    @Override public int    unlockLevel() { return 25; }

    @Override
    public void onActivate(Player player, UUID uuid) {
        onActivate(player, uuid, null);
    }

    @Override
    public void onActivate(Player player, UUID uuid, @Nullable Block target) {
        if (target == null) {
            player.sendActionBar(MINI.deserialize("<red>Perfect Sear: <white>aim at a cooking station."));
            return;
        }
        long remaining = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        if (remaining > 0) {
            player.sendActionBar(MINI.deserialize("<red>Perfect Sear: <white>" + remaining + "s remaining."));
            return;
        }
        service.armPerfectSear(target);
        cooldownManager.setCooldown(uuid, COOLDOWN_ID, Duration.ofSeconds(config.get().perfectSearCooldownSeconds()));
        player.sendActionBar(MINI.deserialize("<gold>Perfect Sear! <white>The next dish here will be perfect."));
        skill.traceActiveUse(uuid, player.getName(), name(), "primed the next dish");
    }

    @Override
    public String selectionStatus(UUID uuid) {
        long s = cooldownManager.getRemainingSeconds(uuid, COOLDOWN_ID);
        return s > 0 ? "<red>" + s + "s" : "<green>Ready";
    }

    @Override
    public StatusLevel statusLevel(UUID uuid) {
        return cooldownManager.isOnCooldown(uuid, COOLDOWN_ID) ? StatusLevel.ON_COOLDOWN : StatusLevel.READY;
    }
}
