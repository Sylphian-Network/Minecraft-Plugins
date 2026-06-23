package net.sylphian.minecraft.skills.command.admin;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Services and helpers passed to every {@code /sylphian-skills} admin subcommand.
 */
public record SkillsAdminContext() {

    /** Shared MiniMessage serializer for authoring operator-facing text. */
    public static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Unwraps a failed async chain to its root cause message for display.
     *
     * @param ex the throwable from an async chain
     * @return the deepest non-null message, or a generic fallback
     */
    public String rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : "An error occurred.";
    }
}
