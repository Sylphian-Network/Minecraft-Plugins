package net.sylphian.minecraft.entities.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.entities.command.SubCommand;
import net.sylphian.minecraft.entities.entity.EntityRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

import static net.sylphian.minecraft.entities.command.SylphianEntitiesCommand.MINI;

/** {@code /sylphian-entities spawn <entity-id> [player]}: spawns a registered entity at the target's location. */
public final class SpawnSubCommand implements SubCommand {

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("spawn")
                .executes((CommandSender sender, CommandArguments _) ->
                        sender.sendMessage(MINI.deserialize("<red>Usage: /sylphian-entities spawn <entity-id> [player]")))
                .then(new NamespacedKeyArgument("entity")
                        .replaceSuggestions(ArgumentSuggestions.strings(_ ->
                                EntityRegistry.allNamespacedIds().toArray(new String[0])))
                        .executesPlayer((Player player, CommandArguments args) ->
                                spawn(player, (NamespacedKey) Objects.requireNonNull(args.get("entity")), player))
                        .then(new EntitySelectorArgument.OnePlayer("player")
                                .executes((CommandSender sender, CommandArguments args) ->
                                        spawn(sender, (NamespacedKey) Objects.requireNonNull(args.get("entity")), (Player) args.get("player")))));
    }

    private void spawn(CommandSender sender, NamespacedKey key, Player target) {
        String entityId = key.asString();

        if (!EntityRegistry.exists(entityId)) {
            sender.sendMessage(MINI.deserialize("<red>Unknown entity '" + entityId + "'. Use tab completion to see available entities."));
            return;
        }

        Optional<Entity> spawned = EntityRegistry.spawn(entityId, target.getLocation());
        if (spawned.isEmpty()) {
            sender.sendMessage(MINI.deserialize("<red>Failed to spawn '" + entityId + "'."));
            return;
        }

        sender.sendMessage(MINI.deserialize("<gray>Spawned <white>" + entityId + " <gray>at <white>" + target.getName() + "<gray>'s location."));
    }
}
