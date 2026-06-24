package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.db.models.ClanWarpModel;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/**
 * {@code /clan warp} (GUI) and its management subcommands: {@code set}, {@code remove},
 * {@code access}, and {@code restrict}.
 */
public final class WarpSubCommand implements SubCommand {

    private static final String[] ITEM_MATERIALS = Arrays.stream(Material.values())
            .filter(material -> !material.isLegacy() && material.isItem())
            .map(Enum::name)
            .toArray(String[]::new);

    private final ClanCommandContext ctx;

    public WarpSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        ArgumentSuggestions<CommandSender> warpNames = ArgumentSuggestions.stringsAsync(info -> {
            if (!(info.sender() instanceof Player player)) {
                return CompletableFuture.completedFuture(new String[0]);
            }
            return ctx.clanCache().get(player.getUniqueId())
                    .map(clan -> ctx.warpService().listWarps(clan.clanId())
                            .thenApply(list -> list.stream().map(ClanWarpModel::name).toArray(String[]::new)))
                    .orElse(CompletableFuture.completedFuture(new String[0]));
        });

        return new LiteralArgument("warp")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requireClan(player);
                    if (clan == null) {
                        return;
                    }
                    ctx.warpMenu().open(player);
                })
                .then(new LiteralArgument("set")
                        .then(new StringArgument("name")
                                .then(new StringArgument("item").replaceSuggestions(ArgumentSuggestions.strings(ITEM_MATERIALS))
                                        .executesPlayer((Player player, CommandArguments args) ->
                                                set(player, (String) args.get("name"), (String) args.get("item"), ""))
                                        .then(new GreedyStringArgument("description")
                                                .executesPlayer((Player player, CommandArguments args) ->
                                                        set(player, (String) args.get("name"), (String) args.get("item"), (String) args.get("description")))))))
                .then(new LiteralArgument("remove")
                        .then(new StringArgument("name").replaceSuggestions(warpNames)
                                .executesPlayer((Player player, CommandArguments args) ->
                                        remove(player, (String) args.get("name")))))
                .then(new LiteralArgument("access")
                        .then(new StringArgument("name").replaceSuggestions(warpNames)
                                .then(new EntitySelectorArgument.OnePlayer("player")
                                        .executesPlayer((Player player, CommandArguments args) ->
                                                access(player, (String) args.get("name"), (Player) args.get("player"))))))
                .then(new LiteralArgument("restrict")
                        .then(new StringArgument("name").replaceSuggestions(warpNames)
                                .executesPlayer((Player player, CommandArguments args) ->
                                        restrict(player, (String) args.get("name")))));
    }

    private void set(Player player, String name, String itemName, String description) {
        Clan clan = ctx.requirePermission(player, ClanPermission.MANAGE_WARP);
        if (clan == null) {
            return;
        }
        Material material = Material.matchMaterial(itemName);
        if (material == null || !material.isItem()) {
            player.sendMessage(Component.text("Unknown item: " + itemName, NamedTextColor.RED));
            return;
        }
        ctx.warpService().saveWarp(clan.clanId(), name, player.getLocation(), material.name(), description)
                .thenRun(() -> player.sendMessage(MINI.deserialize("<green>Warp <white>" + name + " <green>set to your location.")))
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void remove(Player player, String name) {
        Clan clan = ctx.requirePermission(player, ClanPermission.MANAGE_WARP);
        if (clan == null) {
            return;
        }
        ctx.warpService().removeWarp(clan.clanId(), name)
                .thenAccept(removed -> {
                    if (removed) {
                        player.sendMessage(MINI.deserialize("<yellow>Warp <white>" + name + " <yellow>removed."));
                    } else {
                        player.sendMessage(Component.text("No warp named '" + name + "'.", NamedTextColor.RED));
                    }
                })
                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void access(Player player, String name, Player target) {
        Clan clan = ctx.requirePermission(player, ClanPermission.MANAGE_WARP);
        if (clan == null) {
            return;
        }
        if (!clan.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
            return;
        }
        // The leader and Manage Warps holders always have access, so their access list is fixed.
        if (clan.hasPermission(target.getUniqueId(), ClanPermission.MANAGE_WARP)) {
            boolean leader = clan.leaderId().map(target.getUniqueId()::equals).orElse(false);
            player.sendMessage(Component.text(
                    leader
                            ? "You can't change warp access for the clan leader; they always have access."
                            : target.getName() + " already has access to every warp via Manage Warps.",
                    NamedTextColor.RED));
            return;
        }
        java.util.UUID clanId = clan.clanId();
        java.util.UUID targetId = target.getUniqueId();
        ctx.warpService().getWarp(clanId, name).thenCompose(opt -> {
            if (opt.isEmpty()) {
                player.sendMessage(Component.text("No warp named '" + name + "'.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            return ctx.warpService().listAccess(clanId, name).thenCompose(access -> {
                boolean has = access.contains(targetId);
                CompletableFuture<Void> op = has
                        ? ctx.warpService().revokeAccess(clanId, name, targetId)
                        : ctx.warpService().grantAccess(clanId, name, targetId);
                return op.thenRun(() -> player.sendMessage(has
                        ? MINI.deserialize("<yellow>Revoked <white>" + target.getName() + " <yellow>access to <white>" + name + "<yellow>.")
                        : MINI.deserialize("<green>Granted <white>" + target.getName() + " <green>access to <white>" + name + "<green>.")));
            });
        }).exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void restrict(Player player, String name) {
        Clan clan = ctx.requirePermission(player, ClanPermission.MANAGE_WARP);
        if (clan == null) {
            return;
        }
        java.util.UUID clanId = clan.clanId();
        ctx.warpService().getWarp(clanId, name).thenCompose(opt -> {
            if (opt.isEmpty()) {
                player.sendMessage(Component.text("No warp named '" + name + "'.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            boolean nowRestricted = !opt.get().restricted();
            return ctx.warpService().setRestricted(clanId, name, nowRestricted).thenRun(() -> player.sendMessage(nowRestricted
                    ? MINI.deserialize("<yellow>Warp <white>" + name + " <yellow>is now restricted to its access list.")
                    : MINI.deserialize("<green>Warp <white>" + name + " <green>is now public to the clan.")));
        }).exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }
}
