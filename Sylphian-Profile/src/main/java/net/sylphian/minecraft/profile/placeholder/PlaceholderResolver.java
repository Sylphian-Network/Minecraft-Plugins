package net.sylphian.minecraft.profile.placeholder;

import org.bukkit.entity.Player;

/**
 * Resolves a PlaceholderAPI placeholder string for a player.
 * Only instantiated when PlaceholderAPI is present.
 */
public interface PlaceholderResolver {
    /**
     * @param player      the player to resolve for
     * @param placeholder the full placeholder string, e.g. {@code "%sylphian-economy_balance%"}
     * @return the resolved value, or the original string if unresolvable
     */
    String resolve(Player player, String placeholder);
}