package net.sylphian.minecraft.skills.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sylphian.minecraft.skills.service.SkillsService;
import org.bukkit.OfflinePlayer;

/**
 * Exposes skill data as PlaceholderAPI placeholders.
 *
 * <p>Supported placeholders (replace {@code <skill>} with any skill ID, e.g. {@code mining}):
 * <ul>
 *   <li>{@code %sylphian-skills_level_<skill>%}: current level</li>
 *   <li>{@code %sylphian-skills_xp_<skill>%}: total accumulated XP</li>
 *   <li>{@code %sylphian-skills_xp_next_<skill>%}: XP remaining until next level</li>
 *   <li>{@code %sylphian-skills_cap%}: current global level cap</li>
 *   <li>{@code %sylphian-skills_expansion%}: current expansion name</li>
 * </ul>
 *
 * <p>Config is read through the service on every request so placeholders stay
 * accurate after a reload without needing to re-register the expansion.</p>
 */
public final class SkillsPlaceholderExpansion extends PlaceholderExpansion {

    private final SkillsService service;

    /**
     * @param service the skills service, used for both XP data and live config access
     */
    public SkillsPlaceholderExpansion(SkillsService service) {
        this.service = service;
    }

    @Override public String getIdentifier() { return "sylphian-skills"; }
    @Override public String getAuthor()     { return "QuackieMackie"; }
    @Override public String getVersion()    { return "1.0.0"; }
    @Override public boolean persist()      { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Global placeholders (no skill ID needed)
        if (params.equals("cap")) {
            return String.valueOf(service.getConfig().levelCap());
        }
        if (params.equals("expansion")) {
            return service.getConfig().currentExpansion().name();
        }

        // Skill-specific placeholders: "<type>_<skillId>" or "xp_next_<skillId>"
        if (params.startsWith("level_")) {
            String skillId = params.substring("level_".length());
            return String.valueOf(service.getCachedLevel(player.getUniqueId(), skillId));
        }

        if (params.startsWith("xp_next_")) {
            String skillId = params.substring("xp_next_".length());
            long xp = service.getCachedXP(player.getUniqueId(), skillId);
            return String.valueOf(service.getConfig().xpToNextLevel(xp));
        }

        if (params.startsWith("xp_")) {
            String skillId = params.substring("xp_".length());
            return String.valueOf(service.getCachedXP(player.getUniqueId(), skillId));
        }

        return null;
    }
}
