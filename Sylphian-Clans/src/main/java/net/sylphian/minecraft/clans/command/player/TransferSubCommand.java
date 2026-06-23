package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan transfer <player>} — leader-only; hands leadership to another member. */
public final class TransferSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public TransferSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("transfer")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan transfer <player>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            Clan clan = ctx.requireLeader(player);
                            if (clan == null) {
                                return;
                            }
                            Player target = (Player) args.get("player");

                            if (!clan.isMember(target.getUniqueId())) {
                                player.sendMessage(Component.text(target.getName() + " is not in your clan.", NamedTextColor.RED));
                                return;
                            }

                            ctx.clanService().transferLeadership(clan.clanId(), target.getUniqueId())
                                    .thenRun(() -> {
                                        player.sendMessage(MINI.deserialize("<yellow>Leadership transferred to <white>" + target.getName() + "<yellow>."));
                                        target.sendMessage(MINI.deserialize("<green>You are now the leader of <white>" + clan.name() + "<green>!"));
                                    })
                                    .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        }));
    }
}
