package net.sylphian.minecraft.mining.skill;

import net.sylphian.minecraft.skills.api.SkillsProvider;
import org.bukkit.plugin.Plugin;

/**
 * Isolates all Sylphian-Skills class references so they are only linked when
 * Sylphian-Skills is confirmed to be present.
 */
public final class SkillsBridge {

    private final MiningSkill miningSkill;

    /**
     * Registers the mining skill with the skills framework.
     *
     * @param plugin the owning Mining plugin
     */
    public SkillsBridge(Plugin plugin) {
        this.miningSkill = new MiningSkill();
        SkillsProvider.get().registerSkill(miningSkill, plugin);
    }

    /**
     * Removes the mining skill from the registry on plugin disable.
     * Guards against Sylphian-Skills having already unloaded first.
     */
    public void unregister() {
        if (SkillsProvider.isAvailable()) {
            SkillsProvider.get().unregisterSkill("mining");
        }
    }
}
