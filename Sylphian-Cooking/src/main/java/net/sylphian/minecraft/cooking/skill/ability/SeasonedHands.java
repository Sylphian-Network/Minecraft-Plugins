package net.sylphian.minecraft.cooking.skill.ability;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.skill.CookingSkillConfig;
import net.sylphian.minecraft.cooking.skill.trigger.CookingCompleteTrigger;
import net.sylphian.minecraft.skills.skill.PassiveAbility;
import net.sylphian.minecraft.skills.skill.PassiveTrigger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Passive perk unlocked at level 20. Consecutive cooks in the same spot build stacks that raise the
 * Perfect quality weight and XP, decaying when the player moves away or stops cooking.
 */
public final class SeasonedHands implements PassiveAbility {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<CookingSkillConfig> config;
    private final Map<UUID, CookStreak> streaks;

    /**
     * @param config  supplier for the current config snapshot
     * @param streaks the shared per-player streak state, owned by the skill
     */
    public SeasonedHands(Supplier<CookingSkillConfig> config, Map<UUID, CookStreak> streaks) {
        this.config  = config;
        this.streaks = streaks;
    }

    @Override public String id()               { return "cooking:seasoned-hands"; }
    @Override public String name()             { return "Seasoned Hands"; }
    @Override public String description()      { return "Consecutive cooks build stacks that raise quality and XP; decays when you leave or stop."; }
    @Override public int    unlockLevel()      { return 20; }
    @Override public String triggerCondition() { return "On completing a cook in the same spot."; }

    @Override
    public boolean accepts(PassiveTrigger trigger) {
        return trigger instanceof CookingCompleteTrigger;
    }

    @Override
    public void onPassiveTrigger(Player player, UUID uuid, PassiveTrigger trigger) {
        CookingCompleteTrigger complete = (CookingCompleteTrigger) trigger;
        CookingSkillConfig cfg = config.get();

        int stacks = updateStreak(player, uuid, cfg);

        complete.shiftQuality(CookingQuality.PERFECT, stacks * cfg.seasonedHandsQualityPerStack());
        double xpFactor = 1.0 + stacks * (cfg.seasonedHandsXpPerStackPercent() / 100.0);
        complete.multiplyXp(xpFactor);

        trigger.record(name(), "streak " + stacks + "/" + cfg.seasonedHandsMaxStacks(), false);
        player.sendActionBar(MINI.deserialize(
                "<gold>Seasoned Hands: <white>" + stacks + "<gray>/<white>" + cfg.seasonedHandsMaxStacks()
                + " <gray>stacks"));
    }

    /** Increments the streak if the cook continues the same session, otherwise resets it to one. */
    private int updateStreak(Player player, UUID uuid, CookingSkillConfig cfg) {
        Location loc = player.getLocation();
        long now = System.currentTimeMillis();
        CookStreak current = streaks.get(uuid);

        double rangeSq = cfg.seasonedHandsResetDistance() * cfg.seasonedHandsResetDistance();
        boolean continues = current != null
                && current.lastLocation().getWorld() != null
                && current.lastLocation().getWorld().equals(loc.getWorld())
                && current.lastLocation().distanceSquared(loc) <= rangeSq
                && (now - current.lastCookMillis()) <= cfg.seasonedHandsResetSeconds() * 1000L;

        int stacks = continues ? Math.min(current.stacks() + 1, cfg.seasonedHandsMaxStacks()) : 1;
        streaks.put(uuid, new CookStreak(stacks, loc, now));
        return stacks;
    }
}
