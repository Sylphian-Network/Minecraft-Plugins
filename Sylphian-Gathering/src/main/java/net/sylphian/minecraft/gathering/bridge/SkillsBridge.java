package net.sylphian.minecraft.gathering.bridge;

import net.sylphian.minecraft.skills.api.SkillsProvider;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Isolates every reference to Sylphian-Skills, which is an optional dependency.
 * Every call is a no-op (or returns 0) when Skills is absent, so callers never
 * risk a {@link NoClassDefFoundError} or need their own guard.
 */
public final class SkillsBridge {

    private SkillsBridge() {}

    /**
     * Awards XP in a skill if Skills is present and the skill id is set.
     * Must be called on the main thread.
     *
     * @param player  the player receiving XP
     * @param skillId the skill id, or null to skip
     * @param amount  the XP to award; non-positive amounts are skipped
     */
    public static void awardXp(Player player, String skillId, long amount) {
        if (skillId == null || amount <= 0) return;
        if (!SkillsProvider.isAvailable()) return;
        SkillsProvider.get().awardXP(player, skillId, amount);
    }

    /**
     * Returns a player's cached level in a skill, or 0 if Skills is absent or the
     * skill id is null.
     *
     * @param uuid    the player's UUID
     * @param skillId the skill id, or null
     * @return the cached level, or 0
     */
    public static int level(UUID uuid, String skillId) {
        if (skillId == null || !SkillsProvider.isAvailable()) return 0;
        return SkillsProvider.get().getCachedLevel(uuid, skillId);
    }
}
