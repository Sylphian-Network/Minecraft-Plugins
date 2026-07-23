package net.sylphian.minecraft.clans.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.clans.cache.ClanCache;
import net.sylphian.minecraft.clans.command.player.*;
import net.sylphian.minecraft.clans.gui.ClanPermissionMenu;
import net.sylphian.minecraft.clans.gui.ClanWarpMenu;
import net.sylphian.minecraft.clans.service.ClanTeleportWarmupManager;
import net.sylphian.minecraft.clans.service.ClanInviteService;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.ClanWarpService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.entity.Player;

import java.util.List;

import static net.sylphian.minecraft.clans.command.player.ClanCommandContext.MINI;

/**
 * Builds and registers the root {@code /clan} CommandAPI command tree from its subcommand branches.
 */
public final class ClanCommand {

    private static final String PERMISSION = "sylphian.clans.use";

    private final List<SubCommand> subCommands;

    /**
     * @param clanService      the clan business logic service
     * @param inviteService    the in-memory invite store
     * @param territoryService the territory claiming service
     * @param clanCache        the in-memory membership cache
     * @param warmupManager    manages pending teleport warmups
     * @param permissionMenu   the member permission editing GUI
     * @param warpService      the clan warp business logic service
     * @param warpMenu         the clan warp list GUI
     */
    public ClanCommand(ClanService clanService, ClanInviteService inviteService,
                       TerritoryService territoryService, ClanCache clanCache,
                       ClanTeleportWarmupManager warmupManager, ClanPermissionMenu permissionMenu,
                       ClanWarpService warpService, ClanWarpMenu warpMenu) {
        ClanCommandContext ctx = new ClanCommandContext(clanService, inviteService, territoryService, clanCache,
                warmupManager, permissionMenu, warpService, warpMenu);
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
                new WarpSubCommand(ctx),
                new MotdSubCommand(ctx));
    }

    /**
     * Builds the {@code /clan} tree with every subcommand branch and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("clan")
                .withPermission(PERMISSION)
                .withShortDescription("Use clan commands.")
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
                /clan warp
                /clan warp set <name> <item> [description]
                /clan warp remove <name>
                /clan warp access <name> <player>
                /clan warp restrict <name>
                /clan info [clan]
                /clan list"""));
    }
}
