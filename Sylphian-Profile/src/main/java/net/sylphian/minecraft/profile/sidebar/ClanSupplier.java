package net.sylphian.minecraft.profile.sidebar;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Supplies a player's clan tag and name for sidebar display.
 *
 * <p>Backed by Sylphian-Clans only when it is present. {@link ProfileContributor}
 * treats a {@code null} supplier, or a {@code null} return, as "not in a clan".</p>
 */
public interface ClanSupplier {

    /**
     * @param uuid the player's UUID
     * @return a formatted clan string (e.g. {@code "[SYL] Sylphian"}), or null if not in a clan
     */
    @Nullable String formattedClan(UUID uuid);
}