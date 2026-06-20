package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan sethome} — sets the clan home to the sender's current location. */
public final class SetHomeSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public SetHomeSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("sethome")
                .executesPlayer((Player player, CommandArguments _) -> {
                    Clan clan = ctx.requirePermission(player, ClanPermission.SET_HOME);
                    if (clan == null) {
                        return;
                    }
                    ctx.clanService().setHome(clan.clanId(), player.getLocation())
                            .thenRun(() -> player.sendMessage(
                                    MINI.deserialize("<green>Clan home set to your current location.")))
                            .exceptionally(ex -> { player.sendMessage(Component.text(ctx.rootCause(ex), NamedTextColor.RED)); return null; });
                });
    }
}
