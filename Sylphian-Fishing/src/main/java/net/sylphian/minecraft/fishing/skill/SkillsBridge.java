package net.sylphian.minecraft.fishing.skill;

import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.skills.api.SkillsProvider;

/**
 * Isolates all Sylphian-Skills class references so they are only linked when
 * Sylphian-Skills is confirmed to be present.
 *
 * <p>Only instantiate this class after verifying
 * {@code getServer().getPluginManager().getPlugin("Sylphian-Skills") != null}.
 * This prevents {@link NoClassDefFoundError} when Sylphian-Skills is absent.</p>
 */
public final class SkillsBridge {

    private final FishingSkill fishingSkill;

    /**
     * Registers the fishing skill with the skills framework.
     * Called from {@link SylphianFishing#onEnable()} behind the null check.
     *
     * @param plugin the owning Fishing plugin
     */
    public SkillsBridge(SylphianFishing plugin) {
        this.fishingSkill = new FishingSkill(plugin);
        SkillsProvider.get().registerSkill(fishingSkill, plugin);
    }

    /**
     * Removes the fishing skill from the registry on plugin disable.
     * Guards against Sylphian-Skills having already unloaded first.
     */
    public void unregister() {
        if (SkillsProvider.isAvailable()) {
            SkillsProvider.get().unregisterSkill("fishing");
        }
    }

    /**
     * Propagates a config reload to the fishing skill listener.
     * Called from {@link SylphianFishing#reload(org.bukkit.command.CommandSender)}.
     */
    public void reload() {
        fishingSkill.reload();
    }
}
