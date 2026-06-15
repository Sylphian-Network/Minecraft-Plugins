package net.sylphian.minecraft.profile.sidebar;

import net.sylphian.minecraft.profile.UserProfile;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.scoreboard.api.AbstractSidebarContributor;
import net.sylphian.minecraft.scoreboard.api.SidebarLine;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
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
    private final @Nullable BalanceSupplier balanceSupplier;

    /**
     * Constructs a new ProfileContributor.
     *
     * @param profileManager  the in-memory profile cache
     * @param balanceSupplier supplies the player's balance line, or null to omit it
     */
    public ProfileContributor(ProfileManager profileManager, @Nullable BalanceSupplier balanceSupplier) {
        super("sylphian-profile", PRIORITY);
        this.profileManager = profileManager;
        this.balanceSupplier = balanceSupplier;
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

        List<SidebarLine> lines = new ArrayList<>();
        String display = profile.forumUsername() != null ? profile.forumUsername() : profile.mcUsername();
        lines.add(SidebarLine.of("<dark_gray>Welcome <gray>" + display + "!"));
        lines.add(SidebarLine.of("<dark_gray>Playtime: <gray>" + playtime));

        if (balanceSupplier != null) {
            String balance = balanceSupplier.formattedBalance(player.getUniqueId());
            if (balance != null) {
                lines.add(SidebarLine.of("<dark_gray>Balance: <gray>" + balance));
            }
        }

        return lines;
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