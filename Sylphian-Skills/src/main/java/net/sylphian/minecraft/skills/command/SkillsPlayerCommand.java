package net.sylphian.minecraft.skills.command;

import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.skills.gui.SkillsMenu;
import org.bukkit.entity.Player;

/**
 * Builds and registers the player-facing {@code /skills} CommandAPI command.
 *
 * <p>Executing {@code /skills} with no arguments opens the skill browser GUI,
 * showing the player's level and XP progress across every registered skill.</p>
 *
 * <p>Requires {@code sylphian.skills.use} (default: true).</p>
 */
public final class SkillsPlayerCommand {

    private static final String PERMISSION = "sylphian.skills.use";

    private final SkillsMenu skillsMenu;

    /**
     * @param skillsMenu the menu instance to open when the command is executed
     */
    public SkillsPlayerCommand(SkillsMenu skillsMenu) {
        this.skillsMenu = skillsMenu;
    }

    /**
     * Registers the {@code /skills} command with the CommandAPI.
     */
    public void register() {
        new CommandTree("skills")
                .withPermission(PERMISSION)
                .withShortDescription("View your skill levels and XP progress.")
                .executesPlayer((Player player, CommandArguments _) ->
                        skillsMenu.open(player))
                .register();
    }
}
