package net.sylphian.minecraft.skills.skill;

/**
 * Outcome of an {@link ActiveAbility#onActivate} call, returned so the framework
 * can emit the watch trace centrally rather than each ability doing it.
 *
 * @param activated {@code true} if the ability actually took effect, {@code false} if it was
 *                  blocked (cooldown, already active/pending, or an invalid/absent target)
 * @param detail    short trace description of what happened, shown to a debug watcher;
 *                  ignored when {@code activated} is {@code false}
 */
public record ActivationResult(boolean activated, String detail) {

    /**
     * @param detail short trace description of the effect
     * @return a result marking the ability as having taken effect
     */
    public static ActivationResult used(String detail) {
        return new ActivationResult(true, detail);
    }

    /**
     * @return a result marking the activation as blocked, with no effect
     */
    public static ActivationResult blocked() {
        return new ActivationResult(false, "");
    }
}
