package net.sylphian.minecraft.cooking.skill;

import net.sylphian.minecraft.cooking.SylphianCooking;
import net.sylphian.minecraft.cooking.station.CookingStationService;
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

    private final CookingSkill cookingSkill;

    /**
     * Registers the cooking skill with the skills framework and wires the level
     * provider into the station service so quality rolls use the player's skill level.
     *
     * @param plugin  the owning Cooking plugin
     * @param service the cooking station service
     */
    public SkillsBridge(SylphianCooking plugin, CookingStationService service) {
        this.cookingSkill = new CookingSkill(plugin);
        SkillsProvider.get().registerSkill(cookingSkill, plugin);

        service.setLevelProvider(uuid ->
                SkillsProvider.isAvailable()
                        ? SkillsProvider.get().getCachedLevel(uuid, "cooking")
                        : 0);
    }

    /**
     * Removes the cooking skill from the registry on plugin disable.
     * Guards against Sylphian-Skills having already unloaded first.
     */
    public void unregister() {
        if (SkillsProvider.isAvailable()) {
            SkillsProvider.get().unregisterSkill("cooking");
        }
    }

    /**
     * Propagates a config reload to the cooking skill listener.
     * Called from {@link SylphianCooking#reload(org.bukkit.command.CommandSender)}.
     */
    public void reload() {
        cookingSkill.reload();
    }
}
