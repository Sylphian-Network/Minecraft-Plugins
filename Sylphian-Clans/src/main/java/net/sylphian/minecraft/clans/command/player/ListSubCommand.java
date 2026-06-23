package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan list} — lists every clan with member counts. */
public final class ListSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public ListSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("list")
                .executesPlayer((Player player, CommandArguments _) -> ctx.clanService().getAllClans().thenAccept(clans -> {
                    if (clans.isEmpty()) {
                        player.sendMessage(Component.text("No clans exist yet.", NamedTextColor.GRAY));
                        return;
                    }
                    player.sendMessage(MINI.deserialize("<yellow>--- Clans (" + clans.size() + ") ---"));
                    clans.forEach(cl -> player.sendMessage(MINI.deserialize(
                            "<white>" + cl.name() + " <gray>- " + cl.members().size() + " members")));
                }).exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; }));
    }
}
