package net.sylphian.minecraft.clans.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sylphian.minecraft.clans.api.ClanProvider;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

/**
 * Exposes clan data as PlaceholderAPI placeholders.
 *
 * <p>Supported placeholders:
 * <ul>
 *   <li>{@code %sylphian-clans_name%} — the player's clan name, or "" if not in one</li>
 * </ul>
 *
 * <p>Reads from the in-memory clan cache — no database calls on the render path.</p>
 */
public final class ClanPlaceholderExpansion extends PlaceholderExpansion {

    @Override public String getIdentifier() { return "sylphian-clans"; }
    @Override public String getAuthor() { return "QuackieMackie"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (!ClanProvider.isAvailable()) return "";
        Optional<Clan> clan = ClanProvider.get().getClanByPlayerCached(player.getUniqueId());
        return switch (params) {
            case "name" -> clan.map(Clan::name).orElse("");
            default -> null;
        };
    }
}