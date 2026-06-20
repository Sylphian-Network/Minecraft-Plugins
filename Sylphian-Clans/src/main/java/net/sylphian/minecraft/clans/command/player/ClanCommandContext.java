package net.sylphian.minecraft.clans.command.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.service.ClanHomeWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Shared services and helpers passed to every {@code /clan} subcommand.
 *
 * <p>The {@code require*} helpers send the player an error and return {@code null} when a
 * precondition fails, so a subcommand can short-circuit with {@code if (x == null) return ...;}.</p>
 */
public record ClanCommandContext(ClanService clanService, ClanInviteService inviteService,
                                 TerritoryService territoryService, ClanCache clanCache,
                                 ClanHomeWarmupManager warmupManager) {

    /**
     * Shared MiniMessage serializer for authoring player-facing text.
     */
    public static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Shared date formatter for clan creation timestamps.
     */
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    /**
     * @param clanService      the clan business logic service
     * @param inviteService    the in-memory invite store
     * @param territoryService the territory claiming service
     * @param clanCache        the in-memory membership cache
     * @param warmupManager    manages pending home teleport warmups
     */
    public ClanCommandContext {
    }

    /**
     * Returns the player's clan, or sends an error and returns {@code null} if they are not in one.
     *
     * @param player the command sender
     * @return the player's clan, or {@code null}
     */
    public Clan requireClan(Player player) {
        Clan clan = clanCache.get(player.getUniqueId()).orElse(null);
        if (clan == null) {
            player.sendMessage(Component.text("You are not in a clan.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    /**
     * Returns the player's clan if they are the LEADER, or sends an error and returns {@code null}.
     *
     * @param player the command sender
     * @return the player's clan, or {@code null}
     */
    public Clan requireLeader(Player player) {
        Clan clan = requireClan(player);
        if (clan == null) {
            return null;
        }
        if (!clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
            player.sendMessage(Component.text("Only the clan leader can do that.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    /**
     * Returns the player's clan if they hold the given permission (or are LEADER),
     * or sends an error and returns {@code null}.
     *
     * @param player     the command sender
     * @param permission the required clan permission
     * @return the player's clan, or {@code null}
     */
    public Clan requirePermission(Player player, ClanPermission permission) {
        Clan clan = requireClan(player);
        if (clan == null) {
            return null;
        }
        if (!clan.hasPermission(player.getUniqueId(), permission)) {
            player.sendMessage(Component.text("You don't have the " + permission.name() + " permission.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    /**
     * Resolves a player's name from their UUID, falling back to the UUID string if offline.
     *
     * @param uuid the player UUID
     * @return the resolved name, or the UUID string
     */
    public String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        var profile = Bukkit.getOfflinePlayer(uuid);
        return profile.getName() != null ? profile.getName() : uuid.toString();
    }

    /**
     * Unwraps a failed async chain to its root cause message for display.
     *
     * @param ex the throwable from an async chain
     * @return the deepest non-null message, or a generic fallback
     */
    public String rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : "An error occurred.";
    }
}
