package net.sylphian.minecraft.skills.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.skills.command.SubCommand;
import net.sylphian.minecraft.skills.service.SkillsService;
import net.sylphian.minecraft.skills.skill.Skill;
import net.sylphian.minecraft.skills.skill.Watchable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

import static net.sylphian.minecraft.skills.command.admin.SkillsAdminContext.MINI;

/**
 * {@code /sylphian-skills watch <skill> <player>}
 *
 * <p>Toggles a step-by-step passive trace for a player's skill events.
 * While active, the issuing admin receives a message after each tracked
 * event showing what every passive ability contributed. Running the
 * command again for the same player stops the trace.</p>
 */
public final class WatchSubCommand implements SubCommand {

    private final SkillsService service;

    /**
     * @param service the skills service used to look up registered skills
     */
    public WatchSubCommand(SkillsService service) {
        this.service = service;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("watch")
                .then(skillArg()
                        .then(new EntitySelectorArgument.OnePlayer("target")
                                .executes((CommandSender sender, CommandArguments args) -> {
                                    String skillId = (String) args.get("skill");
                                    Player target  = (Player) args.get("target");
                                    if (skillId == null || target == null) return;
                                    handleWatch(sender, skillId, target);
                                })));
    }

    private void handleWatch(CommandSender sender, String skillId, Player target) {
        Optional<Skill> opt = service.getSkill(skillId);
        if (opt.isEmpty()) {
            sender.sendMessage(MINI.deserialize("<red>Unknown skill: <white>" + skillId));
            return;
        }
        if (!(opt.get() instanceof Watchable skill)) {
            sender.sendMessage(MINI.deserialize(
                    "<red>" + skillId + " <white>does not support watch."));
            return;
        }

        UUID uuid = target.getUniqueId();
        if (skill.isWatched(uuid)) {
            skill.unwatch(uuid);
            sender.sendMessage(MINI.deserialize(
                    "<yellow>[Watch] <white>Stopped watching <aqua>" + target.getName()
                    + "<white>'s <aqua>" + skillId + "<white> events."));
        } else {
            skill.watch(uuid, sender);
            sender.sendMessage(MINI.deserialize(
                    "<yellow>[Watch] <white>Now watching <aqua>" + target.getName()
                    + "<white>'s <aqua>" + skillId
                    + "<white> events. Run again to stop."));
        }
    }

    private Argument<String> skillArg() {
        return new StringArgument("skill")
                .replaceSuggestions(ArgumentSuggestions.strings(
                        _ -> service.getSkills().stream()
                                .filter(s -> s instanceof Watchable)
                                .map(Skill::getId)
                                .toArray(String[]::new)));
    }
}
