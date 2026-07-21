package net.sylphian.minecraft.profile.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import org.bukkit.OfflinePlayer;

import java.time.Instant;

/**
 * Exposes player profile data as PlaceholderAPI placeholders.
 */
public final class ProfilePlaceholderExpansion extends PlaceholderExpansion {

    private final ProfileManager profileManager;

    public ProfilePlaceholderExpansion(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override public String getIdentifier() { return "sylphian-profile"; }
    @Override public String getAuthor() { return "QuackieMackie"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        UserProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) return "";
        return switch (params) {
            case "playtime" -> {
                long totalSeconds = profile.playtime() + (Instant.now().getEpochSecond() - profile.lastSeen());
                yield formatPlaytime(totalSeconds);
            }
            case "username" -> profile.mcUsername();
            case "forum_name" -> profile.forumUsername() != null ? profile.forumUsername() : "";
            default -> null;
        };
    }

    private static String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
