package net.sylphian.minecraft.cooking.quality;

import java.util.Map;

/**
 * Stateless utility that combines base quality weights with skill-level bonuses,
 * slot-count bonuses, and passive ability shifts before rolling the final tier.
 */
public final class QualityRoller {

    private QualityRoller() {}

    /**
     * Rolls a quality tier for a completed recipe.
     *
     * <p>Applies bonuses in this order: level bonus, slot bonus, passive shifts.
     * Each step returns a new {@link QualityWeights} instance so the base weights
     * from config are not mutated.</p>
     *
     * @param base           the base weights from config
     * @param level          the cooking skill level of the last interactor (0 if unknown)
     * @param slotCount      the number of ingredient slots required by the recipe (1–5)
     * @param passiveShifts  per-tier weight deltas contributed by passive abilities
     * @param levelBonus     weight shifted from Burnt to Perfect per skill level
     * @param slotBonus      weight shifted from Burnt to Good per slot above one
     * @return the rolled quality tier
     */
    public static CookingQuality roll(QualityWeights base,
                                      int level,
                                      int slotCount,
                                      Map<CookingQuality, Double> passiveShifts,
                                      double levelBonus,
                                      double slotBonus) {
        return base
                .applyLevelBonus(level, levelBonus)
                .applySlotBonus(slotCount, slotBonus)
                .applyShifts(passiveShifts)
                .roll();
    }
}
