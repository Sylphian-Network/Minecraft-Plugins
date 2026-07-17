package net.sylphian.minecraft.logging.skill;

import net.sylphian.minecraft.logging.SylphianLogging;
import net.sylphian.minecraft.skills.api.SkillsProvider;

/**
 * Isolates all Sylphian-Skills class references so they are only linked when
 * Sylphian-Skills is confirmed to be present.
 */
public final class SkillsBridge {

    private final LoggingSkill loggingSkill;

    /**
     * Registers the logging skill with the skills framework.
     *
     * @param plugin the owning Logging plugin
     */
    public SkillsBridge(SylphianLogging plugin) {
        this.loggingSkill = new LoggingSkill(plugin);
        SkillsProvider.get().registerSkill(loggingSkill, plugin);
    }

    /** Re-reads the skill's config snapshot after a plugin reload. */
    public void reload() {
        loggingSkill.reload();
    }

    /**
     * Removes the logging skill from the registry on plugin disable.
     * Guards against Sylphian-Skills having already unloaded first.
     */
    public void unregister() {
        if (SkillsProvider.isAvailable()) {
            SkillsProvider.get().unregisterSkill("logging");
        }
    }
}
