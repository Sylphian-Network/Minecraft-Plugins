package net.sylphian.minecraft.clans.command.admin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;

/**
 * Services and helpers passed to every {@code /sylphian-clans} admin subcommand.
 */
public record ClanAdminContext(ClanService clanService, TerritoryService territoryService) {

    /**
     * Shared MiniMessage serializer for authoring operator-facing text.
     */
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
