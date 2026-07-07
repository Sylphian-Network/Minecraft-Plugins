package net.sylphian.minecraft.clans.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.clans.SylphianClans;
import net.sylphian.minecraft.clans.command.admin.ClanAdminContext;
import net.sylphian.minecraft.clans.command.admin.DisbandSubCommand;
import net.sylphian.minecraft.clans.command.admin.KickSubCommand;
import net.sylphian.minecraft.clans.command.admin.ReloadSubCommand;
import net.sylphian.minecraft.clans.command.admin.UnclaimSubCommand;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.command.CommandSender;

import java.util.List;

import static net.sylphian.minecraft.clans.command.admin.ClanAdminContext.MINI;

/**
 * Builds and registers the operator-only {@code /sylphian-clans} CommandAPI command tree.
 *
 * <p>Provides force-disband, force-kick, and force-unclaim operations that bypass normal
 * permission checks. Requires {@code sylphian.clans.admin}.</p>
 */
public final class ClanAdminCommand {

    private static final String PERMISSION = "sylphian.clans.admin";

    private final List<SubCommand> subCommands;

    /**
     * @param clanService      the clan service for disband and kick operations
     * @param territoryService the territory service for unclaim operations
     * @param plugin           the owning plugin, used by the reload subcommand
     */
    public ClanAdminCommand(ClanService clanService, TerritoryService territoryService, SylphianClans plugin) {
        ClanAdminContext ctx = new ClanAdminContext(clanService, territoryService);
        this.subCommands = List.of(
                new DisbandSubCommand(ctx),
                new KickSubCommand(ctx),
                new UnclaimSubCommand(ctx),
                new ReloadSubCommand(plugin));
    }

    /**
     * Builds the {@code /sylphian-clans} tree with every admin subcommand and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-clans")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative clan commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-clans commands ---
                <white>/sylphian-clans disband <clan_name>
                /sylphian-clans kick <player>
                /sylphian-clans unclaim <world> <chunk_x> <chunk_z>
                /sylphian-clans reload"""));
    }
}
