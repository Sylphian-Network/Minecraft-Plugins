package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan leave} — leaves the sender's clan, disbanding it if they are the sole member. */
public final class LeaveSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public LeaveSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("leave")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requireClan(player);
                    if (clan == null) {
                        return;
                    }

                    if (clan.leaderId().map(player.getUniqueId()::equals).orElse(false)) {
                        if (clan.members().size() > 1) {
                            player.sendMessage(Component.text(
                                    "You are the leader. Transfer leadership first (/clan transfer <player>) or disband (/clan disband).",
                                    NamedTextColor.RED));
                            return;
                        }

                        // Leader is the sole member, disband.
                        ctx.clanService().disbandClan(clan.clanId())
                                .thenRun(() -> player.sendMessage(MINI.deserialize("<red>You were the last member. Clan disbanded.")))
                                .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                        return;
                    }

                    ctx.clanService().removeMember(clan.clanId(), player.getUniqueId())
                            .thenRun(() -> player.sendMessage(MINI.deserialize("<yellow>You left <white>" + clan.name() + "<yellow>.")))
                            .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                });
    }
}
