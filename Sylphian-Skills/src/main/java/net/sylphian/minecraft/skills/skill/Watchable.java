package net.sylphian.minecraft.skills.skill;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Optional capability for skills that support live debug tracing.
 *
 * <p>Skills that implement this interface (typically via {@link AbstractSkill})
 * can be targeted by {@code /sylphian-skills watch}. While a session is active,
 * the watching admin receives a per-event trace of every ability's contribution.</p>
 *
 * <p>Skills that do not need tracing can omit this interface; they will not
 * appear in the {@code watch} command's tab-completion.</p>
 */
public interface Watchable {

    /**
     * Registers an admin to receive debug event traces for a player's skill events.
     * Replaces any existing watcher for that player.
     *
     * @param playerUuid the player whose events to watch
     * @param watcher    the admin to send trace output to
     */
    void watch(UUID playerUuid, CommandSender watcher);

    /**
     * Removes the debug watcher for a player.
     *
     * @param playerUuid the player to stop watching
     */
    void unwatch(UUID playerUuid);

    /**
     * Returns {@code true} if a debug watcher is registered for this player.
     *
     * @param playerUuid the player's UUID
     * @return {@code true} if the player is currently being watched
     */
    boolean isWatched(UUID playerUuid);

    /**
     * Returns the admin watching this player's skill events, or {@code null} if none.
     *
     * @param playerUuid the player's UUID
     * @return the watching admin, or {@code null}
     */
    @Nullable CommandSender getWatcher(UUID playerUuid);

    /**
     * Emits a one-line {@code Active} trace block for a successful ability activation.
     * No-op when the player is not being watched.
     *
     * @param uuid    the activating player's UUID
     * @param subject the player name shown in the line
     * @param ability the ability's display name
     * @param detail  a short MiniMessage description of what happened
     */
    void traceActiveUse(UUID uuid, String subject, String ability, String detail);
}
