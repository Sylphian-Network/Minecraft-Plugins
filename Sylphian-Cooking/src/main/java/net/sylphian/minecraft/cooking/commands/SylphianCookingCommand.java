package net.sylphian.minecraft.cooking.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sylphian.minecraft.cooking.SylphianCooking;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Root administrative command for Sylphian-Cooking ({@code /sylphian-cooking reload}).
 * Requires the {@code sylphian.cooking.admin} permission.
 */
public class SylphianCookingCommand implements BasicCommand {
    private final SylphianCooking plugin;

    public SylphianCookingCommand(SylphianCooking plugin) { this.plugin = plugin; }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length == 0) { sendUsage(sender); return; }
        switch (args[0].toLowerCase()) {
            case "reload" -> plugin.reload(sender);
            default       -> sendUsage(sender);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length <= 1) return List.of("reload");
        return List.of();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("sylphian.cooking.admin");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /sylphian-cooking reload", NamedTextColor.RED));
    }
}
