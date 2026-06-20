package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanMember;
import net.sylphian.minecraft.clans.model.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.DATE_FMT;
import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan info [clan]} — shows the sender's clan, or a named clan. */
public final class InfoSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public InfoSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("info")
                .executesPlayer((Player player, CommandArguments _) ->
                        ctx.clanCache().get(player.getUniqueId()).ifPresentOrElse(
                                clan -> printClanInfo(player, clan),
                                () -> player.sendMessage(Component.text("You are not in a clan. Use /clan info <name> to look up another clan.", NamedTextColor.GRAY))
                        ))
                .then(new StringArgument("clan")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            String name = (String) args.get("clan");
                            ctx.clanService().getClanByName(name).thenAccept(opt ->
                                    opt.ifPresentOrElse(
                                            clan -> printClanInfo(player, clan),
                                            () -> player.sendMessage(Component.text("No clan named '" + name + "' found.", NamedTextColor.RED))
                                    )
                            ).exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }

    /** Loads the clan's claim count, then renders the info panel. */
    private void printClanInfo(Player player, Clan clan) {
        ctx.territoryService().getClaimsForClan(clan.clanId())
                .thenAccept(claims -> render(player, clan, claims.size()))
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void render(Player player, Clan clan, int claimCount) {
        Set<UUID> onlineIds = Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        int total = clan.members().size();
        long online = clan.members().stream().filter(m -> onlineIds.contains(m.playerId())).count();
        String leaderName = clan.leaderId().map(ctx::resolvePlayerName).orElse("unknown");
        int cap = ctx.territoryService().getMaxClaimsPerClan();

        player.sendMessage(MINI.deserialize("<yellow><st>        </st> <white>" + clan.name() + " <yellow><st>        </st>"));
        if (clan.motd() != null && !clan.motd().isBlank()) {
            player.sendMessage(MINI.deserialize("<gray><i>\"<reset>" + clan.motd() + "<gray><i>\""));
        }
        player.sendMessage(MINI.deserialize("<gray>Leader: <gold>" + leaderName));
        player.sendMessage(MINI.deserialize("<gray>Members: <white>" + total + " <gray>(<green>" + online + " online<gray>)"));
        player.sendMessage(MINI.deserialize("<gray>Territory: <white>" + claimCount + " <gray>/ <white>" + cap + " <gray>chunks"));
        player.sendMessage(MINI.deserialize("<gray>Founded: <white>" + DATE_FMT.format(clan.createdAt()) + " <gray>(" + relativeAge(clan.createdAt()) + ")"));
        player.sendMessage(MINI.deserialize("<gray>Clan members:"));

        List<ClanMember> sorted = clan.members().stream()
                .sorted(Comparator
                        .<ClanMember>comparingInt(m -> clanMembersRank(m, onlineIds))
                        .thenComparing(m -> ctx.resolvePlayerName(m.playerId()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        sorted.forEach(m -> player.sendMessage(clanMembersLine(m, onlineIds)));
    }

    /** Sort order: leader first, then online members, then offline members. */
    private int clanMembersRank(ClanMember member, Set<UUID> onlineIds) {
        if (member.role() == ClanRole.LEADER) {
            return 0;
        }
        return onlineIds.contains(member.playerId()) ? 1 : 2;
    }

    private Component clanMembersLine(ClanMember member, Set<UUID> onlineIds) {
        boolean online = onlineIds.contains(member.playerId());
        NamedTextColor nameColor = online ? NamedTextColor.GREEN : NamedTextColor.GRAY;

        String hoverText = "<gray>Role: <white>" + member.role().name() + "<newline>";
        if (!online) {
            hoverText += "<gray>Last online: <white>" + lastOnline(member.playerId()) + "<newline>";
        }
        hoverText += "<gray>Joined: <white>" + DATE_FMT.format(member.joinedAt()) + "<newline>";
        Component hover = MINI.deserialize(hoverText);

        return Component.text(" - " + ctx.resolvePlayerName(member.playerId()), nameColor)
                .hoverEvent(HoverEvent.showText(hover));
    }

    private String relativeAge(Instant createdAt) {
        long days = Duration.between(createdAt, Instant.now()).toDays();
        if (days <= 0) {
            return "today";
        }
        return days + (days == 1 ? " day ago" : " days ago");
    }

    /**
     * Last time the player was seen on this server, as a relative string.
     * Per-server by design: clans are scoped to one server, so cross-server presence is irrelevant here.
     */
    private String lastOnline(UUID playerId) {
        long lastSeen = Bukkit.getOfflinePlayer(playerId).getLastSeen();
        if (lastSeen <= 0L) {
            return "unknown";
        }
        return relativeTime(Instant.ofEpochMilli(lastSeen));
    }

    private String relativeTime(Instant past) {
        Duration since = Duration.between(past, Instant.now());
        long minutes = since.toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = since.toHours();
        if (hours < 24) {
            return hours + "h ago";
        }
        return since.toDays() + "d ago";
    }
}
