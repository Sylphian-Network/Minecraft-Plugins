package net.sylphian.minecraft.logging.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Immutable snapshot of Logging's skill-ability tuning, read from the
 * {@code logging-skill} section of {@code config.yml}. Parse via
 * {@link #from(FileConfiguration, Logger)} and swap the reference on reload.
 *
 * @param timberFallBaseChance         Timber Fall base chance for a full-trunk drop
 * @param timberFallChancePerLevel     Timber Fall added chance per level
 * @param timberFallYieldMultiplier    Timber Fall yield multiplier on a hit
 * @param sapTapperChance              Sap Tapper chance to also drop the sap item
 * @param sapTapperItem                the item id Sap Tapper drops
 * @param woodsmansRhythmWindowSeconds seconds within which consecutive chops keep the streak
 * @param woodsmansRhythmXpPerStack    Woodsman's Rhythm XP bonus per stack
 * @param woodsmansRhythmMaxStacks     Woodsman's Rhythm maximum stacks
 * @param heartwoodStrikeCooldownSeconds Heartwood Strike cooldown
 * @param woodcuttersFrenzyCooldownSeconds Woodcutter's Frenzy cooldown
 * @param woodcuttersFrenzyDurationSeconds Woodcutter's Frenzy buff duration
 * @param woodcuttersFrenzyXpMultiplier    Woodcutter's Frenzy XP multiplier while active
 * @param woodcuttersFrenzyHasteAmplifier  Haste potion amplifier while active (0 = Haste I)
 * @param ancientTimberCooldownSeconds Ancient Timber cooldown
 * @param ancientTimberYieldMultiplier Ancient Timber yield multiplier for the pending chop
 */
public record LoggingSkillConfig(
        double timberFallBaseChance,
        double timberFallChancePerLevel,
        double timberFallYieldMultiplier,
        double sapTapperChance,
        String sapTapperItem,
        int    woodsmansRhythmWindowSeconds,
        double woodsmansRhythmXpPerStack,
        int    woodsmansRhythmMaxStacks,
        int    heartwoodStrikeCooldownSeconds,
        int    woodcuttersFrenzyCooldownSeconds,
        int    woodcuttersFrenzyDurationSeconds,
        double woodcuttersFrenzyXpMultiplier,
        int    woodcuttersFrenzyHasteAmplifier,
        int    ancientTimberCooldownSeconds,
        double ancientTimberYieldMultiplier
) {

    /**
     * Parses the config with safe defaults; a missing key never aborts startup.
     *
     * @param config the loaded file configuration
     * @param logger the logger for range warnings
     * @return the parsed snapshot
     */
    public static LoggingSkillConfig from(FileConfiguration config, Logger logger) {
        int maxStacks = config.getInt("logging-skill.woodsmans-rhythm.max-stacks", 5);
        if (maxStacks < 1) {
            logger.warning("logging-skill.woodsmans-rhythm.max-stacks must be at least 1; using 5.");
            maxStacks = 5;
        }

        return new LoggingSkillConfig(
                config.getDouble("logging-skill.timber-fall.base-chance",           0.15),
                config.getDouble("logging-skill.timber-fall.chance-per-level",       0.005),
                config.getDouble("logging-skill.timber-fall.yield-multiplier",       3.0),
                config.getDouble("logging-skill.sap-tapper.chance",                  0.20),
                config.getString("logging-skill.sap-tapper.item",                    "sylphian-logging:tree_sap"),
                config.getInt   ("logging-skill.woodsmans-rhythm.window-seconds",    8),
                config.getDouble("logging-skill.woodsmans-rhythm.xp-per-stack",      0.10),
                maxStacks,
                config.getInt   ("logging-skill.heartwood-strike.cooldown-seconds",  120),
                config.getInt   ("logging-skill.woodcutters-frenzy.cooldown-seconds", 240),
                config.getInt   ("logging-skill.woodcutters-frenzy.duration-seconds", 30),
                config.getDouble("logging-skill.woodcutters-frenzy.xp-multiplier",   2.0),
                config.getInt   ("logging-skill.woodcutters-frenzy.haste-amplifier", 1),
                config.getInt   ("logging-skill.ancient-timber.cooldown-seconds",    300),
                config.getDouble("logging-skill.ancient-timber.yield-multiplier",    2.0)
        );
    }
}
