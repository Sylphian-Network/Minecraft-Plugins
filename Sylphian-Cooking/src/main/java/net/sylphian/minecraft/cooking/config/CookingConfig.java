package net.sylphian.minecraft.cooking.config;

import net.sylphian.minecraft.cooking.quality.CookingQuality;
import net.sylphian.minecraft.cooking.quality.QualityFormat;
import net.sylphian.minecraft.cooking.quality.QualityWeights;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable snapshot of all core cooking configuration values.
 * Parse via {@link #from(FileConfiguration)}. Swap the reference on reload.
 *
 * @param baseWeights       base probability weights for quality tier rolls
 * @param levelBonus        weight shifted from Burnt to Perfect per skill level
 * @param slotBonus         weight shifted from Burnt to Good per ingredient slot above one
 * @param burntFormat       lore rule for the Burnt tier
 * @param plainFormat       lore rule for the Plain tier
 * @param goodFormat        lore rule for the Good tier
 * @param perfectFormat     lore rule for the Perfect tier
 * @param masteryThreshold  cook count at which a recipe is considered mastered
 * @param masteryBonus      quality weight shifted from Burnt to Perfect on mastered recipes
 * @param masteryMilestones cook counts at which a mastery milestone event fires
 */
public record CookingConfig(
        QualityWeights baseWeights,
        double         levelBonus,
        double         slotBonus,
        QualityFormat  burntFormat,
        QualityFormat  plainFormat,
        QualityFormat  goodFormat,
        QualityFormat  perfectFormat,
        int            masteryThreshold,
        double         masteryBonus,
        Set<Integer>   masteryMilestones
) {

    /**
     * Returns the {@link QualityFormat} for the given tier.
     *
     * @param quality the tier to look up
     * @return the format record for that tier
     */
    public QualityFormat formatFor(CookingQuality quality) {
        return switch (quality) {
            case BURNT   -> burntFormat;
            case PLAIN   -> plainFormat;
            case GOOD    -> goodFormat;
            case PERFECT -> perfectFormat;
        };
    }

    /**
     * Parses a {@link CookingConfig} from the plugin's {@code config.yml}.
     * All keys have safe defaults so a missing or malformed section never aborts startup.
     *
     * @param config the loaded file configuration
     * @return the parsed snapshot
     */
    public static CookingConfig from(FileConfiguration config) {
        ConfigurationSection wSec = config.getConfigurationSection("quality.base-weights");
        QualityWeights baseWeights = new QualityWeights(
                wSec != null ? wSec.getDouble("burnt",   15.0) : 15.0,
                wSec != null ? wSec.getDouble("plain",   55.0) : 55.0,
                wSec != null ? wSec.getDouble("good",    25.0) : 25.0,
                wSec != null ? wSec.getDouble("perfect",  5.0) :  5.0
        );

        double levelBonus = config.getDouble("quality.level-bonus", 0.05);
        double slotBonus  = config.getDouble("quality.slot-bonus",  0.5);

        ConfigurationSection tiers = config.getConfigurationSection("quality.tiers");
        QualityFormat burntFormat   = QualityFormat.from(tiers != null ? tiers.getConfigurationSection("burnt")   : null, "<dark_red>Badly burnt");
        QualityFormat plainFormat   = QualityFormat.from(tiers != null ? tiers.getConfigurationSection("plain")   : null, "<gray>Cooked");
        QualityFormat goodFormat    = QualityFormat.from(tiers != null ? tiers.getConfigurationSection("good")    : null, "<green>Well cooked");
        QualityFormat perfectFormat = QualityFormat.from(tiers != null ? tiers.getConfigurationSection("perfect") : null, "<gold>Perfectly cooked");

        int masteryThreshold = config.getInt("mastery.threshold", 50);
        double masteryBonus  = config.getDouble("mastery.bonus",  5.0);

        List<Integer> milestoneList = config.getIntegerList("mastery.milestones");
        if (milestoneList.isEmpty()) milestoneList = List.of(50, 100);
        Set<Integer> masteryMilestones = new LinkedHashSet<>(milestoneList);

        return new CookingConfig(
                baseWeights, levelBonus, slotBonus,
                burntFormat, plainFormat, goodFormat, perfectFormat,
                masteryThreshold, masteryBonus, masteryMilestones);
    }
}
