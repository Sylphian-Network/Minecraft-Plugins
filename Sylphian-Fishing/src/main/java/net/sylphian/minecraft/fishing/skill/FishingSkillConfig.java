package net.sylphian.minecraft.fishing.skill;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable snapshot of all fishing-skill configuration values.
 *
 * <p>Parse via {@link #from(FileConfiguration)}. Swap the reference on reload;
 * the listener reads through the volatile field in {@link FishingSkill}.</p>
 *
 * @param xpPerCatch                       base XP awarded per Sylphian fish caught
 * @param patientAnglerCooldownSeconds     cooldown for Patient Angler
 * @param patientAnglerMinTicks            minimum hook wait after Patient Angler activates
 * @param patientAnglerMaxTicks            maximum hook wait after Patient Angler activates
 * @param lineMasteryReductionPercent      permanent bite timer reduction from Line Mastery
 * @param doubleHaulCooldownSeconds        cooldown for Double Haul
 * @param steadyCurrentStackReductionPercent  timer reduction added per Steady Current stack
 * @param steadyCurrentMaxStacks           maximum Steady Current stacks
 * @param steadyCurrentRangeBlocks         catch distance to maintain a Steady Current streak
 * @param fishersFrenzyCooldownSeconds     cooldown for Fisher's Frenzy
 * @param fishersFrenzyDurationSeconds     duration of the Fisher's Frenzy buff
 * @param fishersFrenzyReductionPercent    bite timer reduction while Fisher's Frenzy is active
 * @param masterAnglerReductionPercent     additional permanent timer reduction from Master Angler
 * @param masterAnglerXpMultiplier         XP multiplier applied by Master Angler
 */
public record FishingSkillConfig(
        long   xpPerCatch,
        int    patientAnglerCooldownSeconds,
        int    patientAnglerMinTicks,
        int    patientAnglerMaxTicks,
        double lineMasteryReductionPercent,
        int    doubleHaulCooldownSeconds,
        double steadyCurrentStackReductionPercent,
        int    steadyCurrentMaxStacks,
        double steadyCurrentRangeBlocks,
        int    fishersFrenzyCooldownSeconds,
        int    fishersFrenzyDurationSeconds,
        double fishersFrenzyReductionPercent,
        double masterAnglerReductionPercent,
        double masterAnglerXpMultiplier
) {

    /**
     * Parses a {@link FishingSkillConfig} from the fishing plugin's {@code config.yml}.
     * All keys have safe defaults so a missing section never aborts startup.
     *
     * @param config the loaded file configuration
     * @return the parsed snapshot
     */
    public static FishingSkillConfig from(FileConfiguration config) {
        return new FishingSkillConfig(
                config.getLong  ("fishing-skill.xp-per-catch",                              10L),
                config.getInt   ("fishing-skill.patient-angler.cooldown-seconds",            90),
                config.getInt   ("fishing-skill.patient-angler.min-wait-ticks",              60),
                config.getInt   ("fishing-skill.patient-angler.max-wait-ticks",             100),
                config.getDouble("fishing-skill.line-mastery.reduction-percent",            15.0),
                config.getInt   ("fishing-skill.double-haul.cooldown-seconds",              180),
                config.getDouble("fishing-skill.steady-current.stack-reduction-percent",     5.0),
                config.getInt   ("fishing-skill.steady-current.max-stacks",                   5),
                config.getDouble("fishing-skill.steady-current.range-blocks",               15.0),
                config.getInt   ("fishing-skill.fishers-frenzy.cooldown-seconds",           300),
                config.getInt   ("fishing-skill.fishers-frenzy.duration-seconds",            60),
                config.getDouble("fishing-skill.fishers-frenzy.reduction-percent",          60.0),
                config.getDouble("fishing-skill.master-angler.reduction-percent",           20.0),
                config.getDouble("fishing-skill.master-angler.xp-multiplier",               1.5)
        );
    }
}
