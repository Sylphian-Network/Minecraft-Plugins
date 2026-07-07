package net.sylphian.minecraft.fishing.commands;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.fishing.SylphianFishing;
import net.sylphian.minecraft.fishing.commands.admin.ReloadSubCommand;
import net.sylphian.minecraft.fishing.commands.admin.TestEffectSubCommand;
import net.sylphian.minecraft.fishing.commands.admin.TestFishingSubCommand;
import net.sylphian.minecraft.fishing.services.CatchEffectService;
import net.sylphian.minecraft.fishing.services.FishMutationService;
import net.sylphian.minecraft.fishing.services.LootService;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Builds and registers the operator-only {@code /sylphian-fishing} CommandAPI command tree.
 *
 * <p>Subcommands: {@code reload}, {@code test-effect}, {@code test-fishing}.
 * Requires {@code sylphian.fishing.admin}.</p>
 */
public final class SylphianFishingCommand {

    private static final String PERMISSION = "sylphian.fishing.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final List<SubCommand> subCommands;

    /**
     * @param plugin           the owning plugin, used by the reload subcommand
     * @param catchEffectService the service used to apply rarity catch effects
     * @param lootService      the loot service used for fishing simulations
     * @param mutationService  the mutation service used for simulation statistics
     */
    public SylphianFishingCommand(SylphianFishing plugin, CatchEffectService catchEffectService,
                                  LootService lootService, FishMutationService mutationService) {
        this.subCommands = List.of(
                new ReloadSubCommand(plugin),
                new TestEffectSubCommand(catchEffectService),
                new TestFishingSubCommand(lootService, mutationService));
    }

    /**
     * Builds the {@code /sylphian-fishing} tree with every admin subcommand and registers it with the CommandAPI.
     */
    public void register() {
        CommandTree tree = new CommandTree("sylphian-fishing")
                .withPermission(PERMISSION)
                .withShortDescription("Administrative fishing commands.")
                .executes((CommandSender sender, CommandArguments _) -> sendUsage(sender));

        for (SubCommand sub : subCommands) {
            tree.then(sub.branch());
        }

        tree.register();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MINI.deserialize("""
                <yellow>--- /sylphian-fishing commands ---
                <white>/sylphian-fishing reload
                /sylphian-fishing test-effect <rarity>
                /sylphian-fishing test-fishing <count> [biome] [weather] [y] [time]"""));
    }
}
