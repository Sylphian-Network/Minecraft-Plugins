package net.sylphian.minecraft.clans.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.command.player.*;
import net.sylphian.minecraft.clans.gui.ClanPermissionMenu;
import net.sylphian.minecraft.clans.service.ClanHomeWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.entity.Player;

import java.util.List;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/**
 * Builds and registers the root {@code /clan} CommandAPI command tree from its subcommand branches.
 */
public final class ClanCommand {

    private static final String PERMISSION = "sylphian.clans.use";

    private final ClanCommandContext ctx;
    private final List<SubCommand> subCommands;

    /**
     * @param clanService      the clan business logic service
     * @param inviteService    the in-memory invite store
     * @param territoryService the territory claiming service
     * @param clanCache        the in-memory membership cache
     * @param warmupManager    manages pending home teleport warmups
     * @param permissionMenu   the member permission editing GUI
     */
    public ClanCommand(ClanService clanService, ClanInviteService inviteService,
                       TerritoryService territoryService, ClanCache clanCache,
                       ClanHomeWarmupManager warmupManager, ClanPermissionMenu permissionMenu) {
        this.ctx = new ClanCommandContext(clanService, inviteService, territoryService, clanCache, warmupManager, permissionMenu);
        this.subCommands = List.of(
                new CreateSubCommand(ctx),
                new DisbandSubCommand(ctx),
                new InviteSubCommand(ctx),
                new AcceptSubCommand(ctx),
                new DeclineSubCommand(ctx),
                new InvitesSubCommand(ctx),
                new LeaveSubCommand(ctx),
                new KickSubCommand(ctx),
                new TransferSubCommand(ctx),
                new PermissionSubCommand(ctx),
                new ClaimSubCommand(ctx),
                new UnclaimSubCommand(ctx),
                new MapSubCommand(ctx),
                new InfoSubCommand(ctx),
                new ListSubCommand(ctx),
                new SetHomeSubCommand(ctx),
                new HomeSubCommand(ctx),
                new DelHomeSubCommand(ctx),
                new MotdSubCommand(ctx));
    }

    /**
     * Builds the {@code /clan} tree with every subcommand branch and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("clan")
                .withPermission(PERMISSION)
                .executesPlayer((Player player, CommandArguments _) -> sendUsage(player));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(Player player) {
        player.sendMessage(MINI.deserialize("""
                <yellow>--- /clan commands ---
                <white>/clan create <name>
                /clan disband
                /clan invite <player>
                /clan accept <clan>
                /clan decline <clan>
                /clan invites
                /clan leave
                /clan kick <player>
                /clan transfer <player>
                /clan permission <player> grant|revoke|list [perm]
                /clan claim [radius]
                /clan unclaim [all]
                /clan map
                /clan sethome
                /clan home
                /clan delhome
                /clan info [clan]
                /clan list"""));
    }
}
