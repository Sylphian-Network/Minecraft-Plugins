package net.sylphian.minecraft.profile.sidebar;

import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.scoreboard.api.AbstractSidebarContributor;
import net.sylphian.minecraft.scoreboard.api.SidebarLine;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;

/**
 * Contributes player profile information to the sidebar.
 *
 * <p>Shows a welcome message using the player's linked forum username (if any),
 * and live playtime calculated from their cached stored playtime plus their
 * current session duration.</p>
 */
public class ProfileContributor extends AbstractSidebarContributor {

    public static final int PRIORITY = 10;

    private final ProfileManager profileManager;

    /**
     * Constructs a new ProfileContributor.
     *
     * @param profileManager the in-memory profile cache
     */
    public ProfileContributor(ProfileManager profileManager) {
        super("sylphian-profile", PRIORITY);
        this.profileManager = profileManager;
    }

    /**
     * Returns profile lines for the given player.
     * Returns an empty list if the profile is not yet cached.
     *
     * @param player the player to get lines for
     * @return sidebar lines showing forum username and playtime, or empty if profile is unavailable
     */
    @Override
    public List<SidebarLine> getLinesFor(Player player) {
        UserProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) return List.of();

        long totalSeconds = profile.playtime() + (Instant.now().getEpochSecond() - (profile.lastSeen()));
        String playtime = formatPlaytime(totalSeconds);

        return List.of(
                SidebarLine.of("<dark_gray>Welcome <gray>" + profile.forumUsername() + "!"),
                SidebarLine.of("<dark_gray>Playtime: <gray>" + playtime)
        );
    }

    /**
     * Formats a duration in seconds as a human-readable hours and minutes string.
     *
     * @param totalSeconds the duration to format
     * @return formatted string, e.g. {@code "3h 42m"}
     */
    private String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}