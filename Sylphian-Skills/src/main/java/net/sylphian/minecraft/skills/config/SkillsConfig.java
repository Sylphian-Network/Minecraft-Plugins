package net.sylphian.minecraft.skills.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable snapshot of Sylphian-Skills configuration.
 *
 * <p>Levels are split across one or more {@link ExpansionConfig} tiers. Each tier
 * defines its own level count and XP curve. XP within a tier is calculated relative
 * to the start of that tier, so tiers are independently scalable.</p>
 *
 * <p>Parse via {@link #from(FileConfiguration, Logger)}. Services hold a reference
 * to this holder and read through it; swap the reference on reload.</p>
 *
 * @param expansions ordered list of expansion tiers, oldest first
 * @param levelUp    effects shown when a player gains a level
 */
public record SkillsConfig(
        List<ExpansionConfig> expansions,
        LevelUpConfig levelUp
) {

    /**
     * Parses a {@link SkillsConfig} from the plugin's {@code config.yml}.
     * Every key has a safe default so a missing or malformed entry never aborts startup.
     *
     * @param config the loaded file configuration
     * @param logger used to warn on invalid values
     * @return the parsed config snapshot
     */
    public static SkillsConfig from(FileConfiguration config, Logger logger) {
        List<ExpansionConfig> expansions = new ArrayList<>();

        for (Map<?, ?> entry : config.getMapList("expansions")) {
            String name     = entry.get("name") instanceof String s ? s : "Unknown";
            int    levels   = toInt(entry.get("levels"), 50);
            Map<?, ?> curve = entry.get("xp-curve") instanceof Map<?, ?> m ? m : Map.of();
            double base     = toDouble(curve.get("base"),     100.0);
            double exponent = toDouble(curve.get("exponent"), 1.5);

            if (levels < 1) {
                logger.warning("Expansion '" + name + "': levels must be >= 1, defaulting to 50");
                levels = 50;
            }
            if (base <= 0) {
                logger.warning("Expansion '" + name + "': xp-curve.base must be positive, defaulting to 100.0");
                base = 100.0;
            }
            if (exponent <= 0) {
                logger.warning("Expansion '" + name + "': xp-curve.exponent must be positive, defaulting to 1.5");
                exponent = 1.5;
            }

            expansions.add(new ExpansionConfig(name, levels, base, exponent));
        }

        if (expansions.isEmpty()) {
            logger.warning("No expansions configured; using default Base Game (50 levels)");
            expansions.add(new ExpansionConfig("Base Game", 50, 100.0, 1.5));
        }

        return new SkillsConfig(List.copyOf(expansions), LevelUpConfig.from(config));
    }

    /**
     * @return the total level cap across all expansions
     */
    public int levelCap() {
        return expansions.stream().mapToInt(ExpansionConfig::levels).sum();
    }

    /**
     * @return the most recently added expansion, i.e. the active content tier
     */
    public ExpansionConfig currentExpansion() {
        return expansions.get(expansions.size() - 1);
    }

    /**
     * Returns the expansion tier that the given global level falls within.
     *
     * @param globalLevel the global level (1-based)
     * @return the owning expansion tier
     */
    public ExpansionConfig expansionForLevel(int globalLevel) {
        int remaining = globalLevel;
        for (ExpansionConfig tier : expansions) {
            if (remaining <= tier.levels()) return tier;
            remaining -= tier.levels();
        }
        return currentExpansion();
    }

    /**
     * Returns the total accumulated XP required to reach a global level.
     * XP within each tier is calculated relative to the start of that tier.
     *
     * @param globalLevel the target level (1-based)
     * @return total XP required, or 0 for level 0
     */
    public long xpForLevel(int globalLevel) {
        if (globalLevel <= 0) return 0;

        long total     = 0;
        int  remaining = globalLevel;

        for (ExpansionConfig tier : expansions) {
            if (remaining <= tier.levels()) {
                total += xpWithinTier(tier, remaining);
                return total;
            }
            total    += xpWithinTier(tier, tier.levels());
            remaining -= tier.levels();
        }

        return xpForLevel(levelCap());
    }

    /**
     * Derives the current global level from a total accumulated XP value.
     * Result is capped at {@link #levelCap()}.
     *
     * @param xp total accumulated XP
     * @return current global level (0 if no XP)
     */
    public int levelFromXp(long xp) {
        if (xp <= 0) return 0;

        long remaining   = xp;
        int  globalLevel = 0;

        for (ExpansionConfig tier : expansions) {
            long tierTotal = xpWithinTier(tier, tier.levels());
            if (remaining >= tierTotal) {
                remaining    -= tierTotal;
                globalLevel  += tier.levels();
            } else {
                int local = (int) Math.floor(Math.pow(remaining / tier.xpBase(), 1.0 / tier.xpExponent()));
                return globalLevel + Math.min(local, tier.levels());
            }
        }

        return globalLevel;
    }

    /**
     * Returns XP still needed to reach the next level from the given total.
     * Returns 0 if the player is at the level cap.
     *
     * @param currentXp the player's current total XP in this skill
     * @return XP remaining until the next level, or 0 at cap
     */
    public long xpToNextLevel(long currentXp) {
        int current = levelFromXp(currentXp);
        if (current >= levelCap()) return 0;
        return xpForLevel(current + 1) - currentXp;
    }

    /** XP required to reach {@code position} levels into a tier (1-based). */
    private static long xpWithinTier(ExpansionConfig tier, int position) {
        return (long) (tier.xpBase() * Math.pow(position, tier.xpExponent()));
    }

    private static int toInt(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static double toDouble(Object value, double fallback) {
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    /**
     * Configuration for a single expansion tier.
     *
     * @param name       display name shown in skill menus
     * @param levels     how many levels this tier contributes to the cap
     * @param xpBase     base constant in the XP formula for this tier
     * @param xpExponent curve exponent for this tier
     */
    public record ExpansionConfig(
            String name,
            int    levels,
            double xpBase,
            double xpExponent
    ) {}

    /**
     * Immutable snapshot of level-up visual effects.
     *
     * @param soundEnabled  whether to play a sound on level up
     * @param soundName     Bukkit sound key
     * @param soundVolume   audible range multiplier
     * @param soundPitch    playback pitch
     * @param titleEnabled  whether to show a title on level up
     * @param title         MiniMessage title text ({skill}, {level} tokens)
     * @param subtitle      MiniMessage subtitle text ({skill}, {level} tokens)
     * @param fadeIn        title fade-in ticks
     * @param stay          title stay ticks
     * @param fadeOut       title fade-out ticks
     */
    public record LevelUpConfig(
            boolean soundEnabled,
            String  soundName,
            float   soundVolume,
            float   soundPitch,
            boolean titleEnabled,
            String  title,
            String  subtitle,
            int     fadeIn,
            int     stay,
            int     fadeOut
    ) {
        static LevelUpConfig from(FileConfiguration config) {
            return new LevelUpConfig(
                    config.getBoolean("level-up.sound.enabled", true),
                    config.getString("level-up.sound.name", "minecraft:entity.player.levelup"),
                    (float) config.getDouble("level-up.sound.volume", 1.0),
                    (float) config.getDouble("level-up.sound.pitch",  1.0),
                    config.getBoolean("level-up.title.enabled", true),
                    config.getString("level-up.title.title",    "<gold><bold>Level Up!"),
                    config.getString("level-up.title.subtitle", "<yellow>{skill} is now level <gold>{level}<yellow>!"),
                    config.getInt("level-up.title.fade-in",  10),
                    config.getInt("level-up.title.stay",     50),
                    config.getInt("level-up.title.fade-out", 10)
            );
        }
    }
}
