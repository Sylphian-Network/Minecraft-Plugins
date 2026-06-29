package net.sylphian.minecraft.cooking.quality;

import java.util.Map;

/**
 * Probability weights for the four cooking quality tiers.
 *
 * <p>Weights do not need to sum to 1.0; {@link #roll()} normalises them
 * internally. All mutation methods return new instances so the base weights
 * from config are never modified.</p>
 *
 * @param burnt   relative weight for the Burnt tier
 * @param plain   relative weight for the Plain tier
 * @param good    relative weight for the Good tier
 * @param perfect relative weight for the Perfect tier
 */
public record QualityWeights(double burnt, double plain, double good, double perfect) {

    /**
     * Shifts weight from Burnt toward Perfect based on skill level.
     * The shift is capped so Burnt cannot go below zero.
     *
     * @param level         the player's current skill level
     * @param perLevelShift weight moved from Burnt to Perfect per level
     * @return new weights with the level bonus applied
     */
    public QualityWeights applyLevelBonus(int level, double perLevelShift) {
        double shift = Math.min(burnt, level * perLevelShift);
        return new QualityWeights(burnt - shift, plain, good, perfect + shift);
    }

    /**
     * Shifts weight from Burnt toward Good based on ingredient slot count.
     * One slot (the minimum) gives no bonus; each additional slot adds
     * {@code perSlotShift}. The shift is capped so Burnt cannot go below zero.
     *
     * @param slotCount    number of ingredients the recipe requires (1–5)
     * @param perSlotShift weight moved from Burnt to Good per slot above one
     * @return new weights with the slot bonus applied
     */
    public QualityWeights applySlotBonus(int slotCount, double perSlotShift) {
        double shift = Math.min(burnt, (slotCount - 1) * perSlotShift);
        return new QualityWeights(burnt - shift, plain, good + shift, perfect);
    }

    /**
     * Adds the given per-tier deltas to the weights. Each tier is clamped
     * at zero so passives cannot produce negative weights.
     *
     * @param shifts map of tier to weight delta; missing tiers are treated as 0
     * @return new weights with the shifts applied
     */
    public QualityWeights applyShifts(Map<CookingQuality, Double> shifts) {
        return new QualityWeights(
                Math.max(0, burnt   + shifts.getOrDefault(CookingQuality.BURNT,   0.0)),
                Math.max(0, plain   + shifts.getOrDefault(CookingQuality.PLAIN,   0.0)),
                Math.max(0, good    + shifts.getOrDefault(CookingQuality.GOOD,    0.0)),
                Math.max(0, perfect + shifts.getOrDefault(CookingQuality.PERFECT, 0.0))
        );
    }

    /**
     * Rolls a quality tier from the current weights.
     * Weights are normalised to a probability distribution before the roll.
     * Returns {@link CookingQuality#PLAIN} if all weights are zero.
     *
     * @return the rolled quality tier
     */
    public CookingQuality roll() {
        double total = burnt + plain + good + perfect;
        if (total <= 0) return CookingQuality.PLAIN;

        double r = Math.random();
        double cursor = 0;
        cursor += burnt / total;   if (r < cursor) return CookingQuality.BURNT;
        cursor += plain / total;   if (r < cursor) return CookingQuality.PLAIN;
        cursor += good  / total;   if (r < cursor) return CookingQuality.GOOD;
        return CookingQuality.PERFECT;
    }
}
