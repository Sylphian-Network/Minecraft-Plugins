package net.sylphian.minecraft.mining.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Immutable snapshot of Mining's skill-ability tuning, read from the
 * {@code mining-skill} section of {@code config.yml}. Parse via
 * {@link #from(FileConfiguration, Logger)} and swap the reference on reload.
 *
 * @param oreSenseBaseChance        Ore Sense base chance to drop a bonus base-loot unit
 * @param oreSenseChancePerLevel    Ore Sense added chance per skill level
 * @param mineralogistBaseBonus     Mineralogist XP bonus on any enriched (modified) node
 * @param mineralogistPerYieldBonus Mineralogist extra XP bonus per +1.0 of the modifier's yield multiplier
 * @param steadyRhythmWindowSeconds seconds within which consecutive mines keep the Steady Rhythm streak
 * @param steadyRhythmYieldPerStack Steady Rhythm yield bonus per stack
 * @param steadyRhythmMaxStacks     Steady Rhythm maximum stacks
 * @param veinSurgeCooldownSeconds  Vein Surge cooldown
 * @param veinSurgeYieldMultiplier  Vein Surge yield multiplier for the pending mine
 * @param prospectorsEyeDurationSeconds Prospector's Eye buff duration
 * @param prospectorsEyeCooldownSeconds Prospector's Eye cooldown
 * @param motherlodeCooldownSeconds Motherlode cooldown
 * @param motherlodeExtraRolls      Motherlode extra loot-table rolls
 */
public record MiningSkillConfig(
        double oreSenseBaseChance,
        double oreSenseChancePerLevel,
        double mineralogistBaseBonus,
        double mineralogistPerYieldBonus,
        int    steadyRhythmWindowSeconds,
        double steadyRhythmYieldPerStack,
        int    steadyRhythmMaxStacks,
        int    veinSurgeCooldownSeconds,
        double veinSurgeYieldMultiplier,
        int    prospectorsEyeDurationSeconds,
        int    prospectorsEyeCooldownSeconds,
        int    motherlodeCooldownSeconds,
        int    motherlodeExtraRolls
) {

    /**
     * Parses the config with safe defaults; a missing key never aborts startup.
     *
     * @param config the loaded file configuration
     * @param logger the logger for range warnings
     * @return the parsed snapshot
     */
    public static MiningSkillConfig from(FileConfiguration config, Logger logger) {
        int maxStacks = config.getInt("mining-skill.steady-rhythm.max-stacks", 5);
        if (maxStacks < 1) {
            logger.warning("mining-skill.steady-rhythm.max-stacks must be at least 1; using 5.");
            maxStacks = 5;
        }

        return new MiningSkillConfig(
                config.getDouble("mining-skill.ore-sense.base-chance",           0.10),
                config.getDouble("mining-skill.ore-sense.chance-per-level",       0.005),
                config.getDouble("mining-skill.mineralogist.base-bonus",         0.20),
                config.getDouble("mining-skill.mineralogist.per-yield-bonus",    0.20),
                config.getInt   ("mining-skill.steady-rhythm.window-seconds",     8),
                config.getDouble("mining-skill.steady-rhythm.yield-per-stack",    0.10),
                maxStacks,
                config.getInt   ("mining-skill.vein-surge.cooldown-seconds",      90),
                config.getDouble("mining-skill.vein-surge.yield-multiplier",      2.0),
                config.getInt   ("mining-skill.prospectors-eye.duration-seconds", 45),
                config.getInt   ("mining-skill.prospectors-eye.cooldown-seconds", 180),
                config.getInt   ("mining-skill.motherlode.cooldown-seconds",      300),
                config.getInt   ("mining-skill.motherlode.extra-rolls",           2)
        );
    }
}
