package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanMember;
import net.sylphian.minecraft.clans.model.ClanPermission;
import net.sylphian.minecraft.clans.model.ClanRole;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/**
 * {@code /clan permission <player> grant|revoke <permission>} and {@code /clan permission <player> list}.
 */
public final class PermissionSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public PermissionSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        ArgumentSuggestions<CommandSender> permSuggestions = ArgumentSuggestions.strings(_ ->
                Arrays.stream(ClanPermission.values()).map(ClanPermission::name).toArray(String[]::new));

        return new LiteralArgument("permission")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan permission <player> grant|revoke|list <permission>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .then(new LiteralArgument("list")
                                .executesPlayer((Player player, CommandArguments args) ->
                                        list(player, (Player) args.get("player"))))
                        .then(new LiteralArgument("grant")
                                .then(new StringArgument("permission").replaceSuggestions(permSuggestions)
                                        .executesPlayer((Player player, CommandArguments args) ->
                                                grant(player, (Player) args.get("player"), (String) args.get("permission")))))
                        .then(new LiteralArgument("revoke")
                                .then(new StringArgument("permission").replaceSuggestions(permSuggestions)
                                        .executesPlayer((Player player, CommandArguments args) ->
                                                revoke(player, (Player) args.get("player"), (String) args.get("permission"))))));
    }

    /** Resolves the actor's clan and verifies the target is a member, or messages and returns {@code null}. */
    private Clan resolveClan(Player actor, Player target) {
        Clan clan = ctx.requireClan(actor);
        if (clan == null) {
            return null;
        }
        if (!clan.isMember(target.getUniqueId())) {
            actor.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
            return null;
        }
        return clan;
    }

    private void list(Player actor, Player target) {
        Clan clan = resolveClan(actor, target);
        if (clan == null) {
            return;
        }
        ClanMember targetMember = clan.getMember(target.getUniqueId()).orElseThrow();
        if (targetMember.role() == ClanRole.LEADER) {
            actor.sendMessage(MINI.deserialize("<gray>" + target.getName() + " is the LEADER and has all permissions."));
        } else {
            String perms = targetMember.permissions().stream()
                    .map(ClanPermission::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            actor.sendMessage(MINI.deserialize("<gray>Permissions for <white>" + target.getName() + "<gray>: <white>"
                    + (perms.isEmpty() ? "none" : perms)));
        }
    }

    private void grant(Player actor, Player target, String permName) {
        Clan clan = resolveClan(actor, target);
        if (clan == null) {
            return;
        }
        ClanPermission permission = parse(actor, permName);
        if (permission == null) {
            return;
        }
        ctx.clanService().grantPermission(actor.getUniqueId(), target.getUniqueId(), permission)
                .thenRun(() -> {
                    actor.sendMessage(MINI.deserialize("<green>Granted <white>" + permission.name() + " <green>to <white>" + target.getName() + "<green>."));
                    target.sendMessage(MINI.deserialize("<green>You were granted <white>" + permission.name() + " <green>in your clan."));
                })
                .exceptionally(ex -> { actor.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private void revoke(Player actor, Player target, String permName) {
        Clan clan = resolveClan(actor, target);
        if (clan == null) {
            return;
        }
        ClanPermission permission = parse(actor, permName);
        if (permission == null) {
            return;
        }
        ctx.clanService().revokePermission(actor.getUniqueId(), target.getUniqueId(), permission)
                .thenRun(() -> {
                    actor.sendMessage(MINI.deserialize("<yellow>Revoked <white>" + permission.name() + " <yellow>from <white>" + target.getName() + "<yellow>."));
                    target.sendMessage(MINI.deserialize("<yellow>Your <white>" + permission.name() + " <yellow>permission was revoked."));
                })
                .exceptionally(ex -> { actor.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
    }

    private ClanPermission parse(Player actor, String raw) {
        try {
            return ClanPermission.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            actor.sendMessage(Component.text("Unknown permission: " + raw, NamedTextColor.RED));
            return null;
        }
    }
}
