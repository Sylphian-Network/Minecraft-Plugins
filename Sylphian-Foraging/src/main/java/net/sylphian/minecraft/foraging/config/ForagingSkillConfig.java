package net.sylphian.minecraft.foraging.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Immutable snapshot of Foraging's skill-ability tuning, read from the
 * {@code foraging-skill} section of {@code config.yml}. Parse via
 * {@link #from(FileConfiguration, Logger)} and swap the reference on reload.
 *
 * @param gentleTouchBaseChance        Gentle Touch base chance to not deplete the node
 * @param gentleTouchChancePerLevel    Gentle Touch added chance per level
 * @param herbalistChance              Herbalist chance to also drop the herb item
 * @param herbalistItem                the item id Herbalist drops
 * @param wildAbundanceWindowSeconds   seconds a harvested node id counts toward variety
 * @param wildAbundanceYieldPerVariety Wild Abundance yield bonus per distinct node id
 * @param wildAbundanceMaxVariety      Wild Abundance distinct-node cap
 * @param regrowthCooldownSeconds      Regrowth cooldown
 * @param regrowthDurationSeconds      Regrowth buff duration
 * @param regrowthRespawnSeconds       respawn time forced on depleted nodes while Regrowth is active
 * @param foragersVigourCooldownSeconds Forager's Vigour cooldown
 * @param foragersVigourDurationSeconds Forager's Vigour buff duration
 * @param foragersVigourXpMultiplier    Forager's Vigour XP multiplier while active
 * @param foragersVigourSaturationAmplifier Saturation amplifier while active (0 = Saturation I)
 * @param verdantBlessingCooldownSeconds Verdant Blessing cooldown
 * @param verdantBlessingRadius         Verdant Blessing search radius in blocks
 */
public record ForagingSkillConfig(
        double gentleTouchBaseChance,
        double gentleTouchChancePerLevel,
        double herbalistChance,
        String herbalistItem,
        int    wildAbundanceWindowSeconds,
        double wildAbundanceYieldPerVariety,
        int    wildAbundanceMaxVariety,
        int    regrowthCooldownSeconds,
        int    regrowthDurationSeconds,
        int    regrowthRespawnSeconds,
        int    foragersVigourCooldownSeconds,
        int    foragersVigourDurationSeconds,
        double foragersVigourXpMultiplier,
        int    foragersVigourSaturationAmplifier,
        int    verdantBlessingCooldownSeconds,
        int    verdantBlessingRadius
) {

    /**
     * Parses the config with safe defaults; a missing key never aborts startup.
     *
     * @param config the loaded file configuration
     * @param logger the logger for range warnings
     * @return the parsed snapshot
     */
    public static ForagingSkillConfig from(FileConfiguration config, Logger logger) {
        int regrowthRespawn = config.getInt("foraging-skill.regrowth.respawn-seconds", 3);
        if (regrowthRespawn < 1) {
            logger.warning("foraging-skill.regrowth.respawn-seconds must be at least 1; using 3.");
            regrowthRespawn = 3;
        }

        return new ForagingSkillConfig(
                config.getDouble("foraging-skill.gentle-touch.base-chance",           0.15),
                config.getDouble("foraging-skill.gentle-touch.chance-per-level",       0.005),
                config.getDouble("foraging-skill.herbalist.chance",                    0.20),
                config.getString("foraging-skill.herbalist.item",                      "sylphian-foraging:wild_herb"),
                config.getInt   ("foraging-skill.wild-abundance.window-seconds",       30),
                config.getDouble("foraging-skill.wild-abundance.yield-per-variety",    0.10),
                config.getInt   ("foraging-skill.wild-abundance.max-variety",          4),
                config.getInt   ("foraging-skill.regrowth.cooldown-seconds",           120),
                config.getInt   ("foraging-skill.regrowth.duration-seconds",           30),
                regrowthRespawn,
                config.getInt   ("foraging-skill.foragers-vigour.cooldown-seconds",    240),
                config.getInt   ("foraging-skill.foragers-vigour.duration-seconds",    60),
                config.getDouble("foraging-skill.foragers-vigour.xp-multiplier",       2.0),
                config.getInt   ("foraging-skill.foragers-vigour.saturation-amplifier", 0),
                config.getInt   ("foraging-skill.verdant-blessing.cooldown-seconds",   300),
                config.getInt   ("foraging-skill.verdant-blessing.radius",             12)
        );
    }
}
