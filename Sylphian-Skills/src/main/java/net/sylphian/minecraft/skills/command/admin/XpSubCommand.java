package net.sylphian.minecraft.skills.command.admin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.sylphian.minecraft.skills.command.SubCommand;
import net.sylphian.minecraft.skills.service.SkillsService;
import net.sylphian.minecraft.skills.skill.Skill;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.sylphian.minecraft.skills.command.admin.SkillsAdminContext.MINI;

/**
 * {@code /sylphian-skills xp <add|set|remove> <player> <skill> <amount>}
 *
 * <p>Direct XP manipulation for an online target player. All three operations update
 * the in-memory cache immediately and write through to the database asynchronously.
 * No level-up events or title effects are fired; this is a silent admin override.</p>
 */
public final class XpSubCommand implements SubCommand {

    private final SkillsService service;

    /**
     * @param service the skills service used for XP read and write operations
     */
    public XpSubCommand(SkillsService service) {
        this.service = service;
    }

    @Override
    public Argument<?> branch() {
        return new LiteralArgument("xp")
                .then(addBranch())
                .then(setBranch())
                .then(removeBranch());
    }

    private Argument<?> addBranch() {
        return new LiteralArgument("add")
                .then(new EntitySelectorArgument.OnePlayer("target")
                        .then(skillArg()
                                .then(new LongArgument("amount", 1)
                                        .executes((CommandSender sender, CommandArguments args) -> {
                                            Player target  = (Player) args.get("target");
                                            String skillId = (String) args.get("skill");
                                            long   amount  = (Long) Objects.requireNonNull(args.get("amount"));
                                            if (target == null || skillId == null) return;
                                            service.adminAddXP(target.getUniqueId(), skillId, amount)
                                                    .thenAccept(after -> sender.sendMessage(MINI.deserialize(
                                                            "<green>Added <white>" + amount + " XP <green>to <white>"
                                                                    + target.getName() + "<green>'s <white>"
                                                                    + skillId + "<green>. Total: <white>" + after)))
                                                    .exceptionally(ex -> {
                                                        sender.sendMessage(MINI.deserialize(
                                                                "<red>Failed: " + SkillsAdminContext.rootCause(ex)));
                                                        return null;
                                                    });
                                        }))));
    }

    private Argument<?> setBranch() {
        return new LiteralArgument("set")
                .then(new EntitySelectorArgument.OnePlayer("target")
                        .then(skillArg()
                                .then(new LongArgument("amount", 0)
                                        .executes((CommandSender sender, CommandArguments args) -> {
                                            Player target  = (Player) args.get("target");
                                            String skillId = (String) args.get("skill");
                                            long   amount  = (Long) Objects.requireNonNull(args.get("amount"));
                                            if (target == null || skillId == null) return;
                                            service.adminSetXP(target.getUniqueId(), skillId, amount)
                                                    .thenAccept(after -> sender.sendMessage(MINI.deserialize(
                                                            "<green>Set <white>" + target.getName()
                                                                    + "<green>'s <white>" + skillId
                                                                    + "<green> XP to <white>" + after)))
                                                    .exceptionally(ex -> {
                                                        sender.sendMessage(MINI.deserialize(
                                                                "<red>Failed: " + SkillsAdminContext.rootCause(ex)));
                                                        return null;
                                                    });
                                        }))));
    }

    private Argument<?> removeBranch() {
        return new LiteralArgument("remove")
                .then(new EntitySelectorArgument.OnePlayer("target")
                        .then(skillArg()
                                .then(new LongArgument("amount", 1)
                                        .executes((CommandSender sender, CommandArguments args) -> {
                                            Player target  = (Player) args.get("target");
                                            String skillId = (String) args.get("skill");
                                            long   amount  = (Long) Objects.requireNonNull(args.get("amount"));
                                            if (target == null || skillId == null) return;
                                            service.adminRemoveXP(target.getUniqueId(), skillId, amount)
                                                    .thenAccept(after -> sender.sendMessage(MINI.deserialize(
                                                            "<green>Removed <white>" + amount + " XP <green>from <white>"
                                                                    + target.getName() + "<green>'s <white>"
                                                                    + skillId + "<green>. Total: <white>" + after)))
                                                    .exceptionally(ex -> {
                                                        sender.sendMessage(MINI.deserialize(
                                                                "<red>Failed: " + SkillsAdminContext.rootCause(ex)));
                                                        return null;
                                                    });
                                        }))));
    }

    private Argument<String> skillArg() {
        return new StringArgument("skill")
                .replaceSuggestions(ArgumentSuggestions.strings(
                        _ -> service.getSkills().stream()
                                .map(Skill::getId)
                                .toArray(String[]::new)));
    }
}
