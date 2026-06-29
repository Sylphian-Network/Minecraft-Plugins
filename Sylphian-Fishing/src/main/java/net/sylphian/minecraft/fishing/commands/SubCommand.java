package net.sylphian.minecraft.fishing.commands;

import dev.jorel.commandapi.arguments.Argument;

/**
 * A single subcommand contributing one literal branch to a command tree.
 */
public interface SubCommand {

    /**
     * @return the built literal argument branch for this subcommand
     */
    Argument<?> branch();
}
