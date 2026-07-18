package net.sylphian.minecraft.foraging.skill;

import net.sylphian.minecraft.foraging.SylphianForaging;
import net.sylphian.minecraft.skills.api.SkillsProvider;

/**
 * Isolates all Sylphian-Skills class references so they are only linked when
 * Sylphian-Skills is confirmed to be present.
 */
public final class SkillsBridge {

    private final ForagingSkill foragingSkill;

    /**
     * Registers the foraging skill with the skills framework.
     *
     * @param plugin the owning Foraging plugin
     */
    public SkillsBridge(SylphianForaging plugin) {
        this.foragingSkill = new ForagingSkill(plugin);
        SkillsProvider.get().registerSkill(foragingSkill, plugin);
    }

    /** Re-reads the skill's config snapshot after a plugin reload. */
    public void reload() {
        foragingSkill.reload();
    }

    /**
     * Removes the foraging skill from the registry on plugin disable.
     * Guards against Sylphian-Skills having already unloaded first.
     */
    public void unregister() {
        if (SkillsProvider.isAvailable()) {
            SkillsProvider.get().unregisterSkill("foraging");
        }
    }
}
