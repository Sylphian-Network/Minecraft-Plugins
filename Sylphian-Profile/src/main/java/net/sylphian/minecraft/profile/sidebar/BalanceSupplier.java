package net.sylphian.minecraft.profile.sidebar;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Supplies a player's formatted balance for sidebar display.
 *
 * <p>Backed by an economy implementation only when Sylphian-Economy is present.
 * {@link ProfileContributor} treats a {@code null} supplier, or a {@code null}
 * return, as "no balance to show".</p>
 */
public interface BalanceSupplier {

    /**
     * @param uuid the player's UUID
     * @return the formatted balance including the currency symbol, or null if not yet known
     */
    @Nullable String formattedBalance(UUID uuid);
}
