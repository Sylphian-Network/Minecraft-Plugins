package net.sylphian.velocity.verify.model;

/**
 * A read-only snapshot of a player's current rate-limit state, used by admin tooling.
 *
 * @param attempts                failed verification attempts currently recorded against this UUID
 * @param strikes                 consecutive failed verification checks currently recorded against this UUID
 * @param cooldownRemainingMillis milliseconds left on an active brute-force cooldown, or 0 if none
 */
public record VerificationStatus(int attempts, int strikes, long cooldownRemainingMillis) {
}
