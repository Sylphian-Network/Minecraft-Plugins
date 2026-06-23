package net.sylphian.minecraft.clans.command.player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.clans.command.SubCommand;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.model.ClanPermission;
import org.bukkit.entity.Player;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/** {@code /clan invite <player>} — invites an online player to the sender's clan. */
public final class InviteSubCommand implements SubCommand {

    private final ClanCommandContext ctx;

    public InviteSubCommand(ClanCommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("invite")
                .executesPlayer((Player player, CommandArguments _) ->
                        player.sendMessage(MINI.deserialize("<red>Usage: /clan invite <player>")))
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((Player player, CommandArguments args) -> {
                            Clan clan = ctx.requirePermission(player, ClanPermission.INVITE_MEMBERS);
                            if (clan == null) {
                                return;
                            }
                            Player target = (Player) args.get("player");

                            if (clan.isMember(target.getUniqueId())) {
                                player.sendMessage(Component.text(target.getName() + " is already in your clan.", NamedTextColor.RED));
                                return;
                            }
                            if (ctx.clanCache().get(target.getUniqueId()).isPresent()) {
                                player.sendMessage(Component.text(target.getName() + " is already in another clan.", NamedTextColor.RED));
                                return;
                            }

                            boolean added = ctx.inviteService().addInvite(clan.clanId(), clan.name(), player.getUniqueId(), target.getUniqueId());
                            if (!added) {
                                player.sendMessage(Component.text(target.getName() + " already has a pending invite from your clan.", NamedTextColor.RED));
                                return;
                            }
                            player.sendMessage(MINI.deserialize("<green>Invited <white>" + target.getName() + " <green>to your clan."));

                            Component acceptButton = Component.text(" [Accept]", NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/clan accept " + clan.name()))
                                    .hoverEvent(HoverEvent.showText(
                                            Component.text("Click to join " + clan.name(), NamedTextColor.YELLOW)));

                            target.sendMessage(Component.text()
                                    .append(MINI.deserialize("<green>You have been invited to join <white>" + clan.name()
                                            + " <green>by <white>" + player.getName() + "<green>."))
                                    .append(acceptButton)
                                    .build());
                        }));
    }
}
