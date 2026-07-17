package net.sylphian.velocity.verify.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sylphian.velocity.verify.VerifyVelocity;
import net.sylphian.velocity.verify.model.VerificationStatus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Admin tooling for Sylphian-Verify, gated behind {@code sylphian.verify.admin}.
 * Supports {@code /verify status}, {@code /verify reset}, and {@code /verify reload}.
 */
public class VerifyCommand implements SimpleCommand {

    private static final String PERMISSION = "sylphian.verify.admin";
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final VerifyVelocity plugin;

    public VerifyCommand(VerifyVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> handleStatus(source, args);
            case "reset" -> handleReset(source, args);
            case "reload" -> handleReload(source);
            default -> sendUsage(source);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return List.of("status", "reset", "reload");
        }
        return List.of();
    }

    private void handleStatus(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(MINI.deserialize("<red>Usage: /verify status <player|uuid>"));
            return;
        }

        UUID uuid = resolveUuid(args[1]);
        if (uuid == null) {
            sendUnresolved(source, args[1]);
            return;
        }

        VerificationStatus status = plugin.getVerifyManager().getStatus(uuid);
        source.sendMessage(MINI.deserialize("""
                        <gray>Verification status for <white><uuid></white>
                        <gray>Failed attempts: <white><attempts></white>
                        <gray>Strikes: <white><strikes></white>
                        <gray>Cooldown remaining: <white><cooldown></white>""",
                Placeholder.unparsed("uuid", uuid.toString()),
                Placeholder.unparsed("attempts", String.valueOf(status.attempts())),
                Placeholder.unparsed("strikes", String.valueOf(status.strikes())),
                Placeholder.unparsed("cooldown", formatDuration(status.cooldownRemainingMillis()))));
    }

    private void handleReset(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(MINI.deserialize("<red>Usage: /verify reset <player|uuid>"));
            return;
        }

        UUID uuid = resolveUuid(args[1]);
        if (uuid == null) {
            sendUnresolved(source, args[1]);
            return;
        }

        plugin.getVerifyManager().resetAll(uuid);
        source.sendMessage(MINI.deserialize("<green>Cleared verification rate-limit state for <white><uuid></white>.",
                Placeholder.unparsed("uuid", uuid.toString())));
    }

    private void handleReload(CommandSource source) {
        plugin.reload();
        source.sendMessage(MINI.deserialize(
                "<green>Sylphian-Verify config reloaded. <gray>(api_key, api_base_url, and the cache "
                        + "expiry windows still require a restart to change.)"));
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(MINI.deserialize("""
                <yellow>/verify status <player|uuid>
                <yellow>/verify reset <player|uuid>
                <yellow>/verify reload"""));
    }

    private void sendUnresolved(CommandSource source, String target) {
        source.sendMessage(MINI.deserialize(
                "<red>Could not resolve <white><target></white> to a UUID. The player must be online, or pass a raw UUID.",
                Placeholder.unparsed("target", target)));
    }

    /**
     * Resolves a raw UUID string, falling back to an online player's username.
     * Offline players who aren't currently connected can only be targeted by UUID.
     *
     * @param input a UUID string or an online player's username
     * @return the resolved UUID, or null if it couldn't be resolved
     */
    private UUID resolveUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException notAUuid) {
            return plugin.getProxy().getPlayer(input).map(Player::getUniqueId).orElse(null);
        }
    }

    private String formatDuration(long millis) {
        if (millis <= 0) {
            return "none";
        }
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
