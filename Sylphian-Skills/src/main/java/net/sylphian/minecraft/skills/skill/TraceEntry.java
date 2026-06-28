package net.sylphian.minecraft.skills.skill;

/**
 * A single recorded contribution from a passive ability to a {@link PassiveTrigger} token.
 *
 * <p>Passives call {@link PassiveTrigger#record} after writing their output into the trigger.
 * The owning skill reads these entries after {@link AbstractSkill#firePassives} returns and
 * forwards the log to any registered debug watcher.</p>
 *
 * @param source      display name of the contributing ability
 * @param description short human-readable description of what the ability did
 * @param active       {@code true} if the contribution came from an active ability
 */
public record TraceEntry(String source, String description, boolean active) {

    /**
     * Convenience constructor for passive contributions; sets {@code active} to {@code false}.
     *
     * @param source      display name of the contributing ability
     * @param description short human-readable description of what the ability did
     */
    public TraceEntry(String source, String description) {
        this(source, description, false);
    }
}
