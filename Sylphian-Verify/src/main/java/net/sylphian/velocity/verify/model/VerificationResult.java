package net.sylphian.velocity.verify.model;

import net.kyori.adventure.text.Component;

/**
 * Represents the final decision of a verification check on Velocity.
 *
 * @param allowed     whether the player is permitted to connect
 * @param kickMessage the component message to display if denied
 * @param identity    the player's identity details if allowed
 */
public record VerificationResult(boolean allowed, Component kickMessage, PlayerIdentity identity) {

    /**
     * Creates an allowed result.
     *
     * @param identity the verified identity
     * @return a successful VerificationResult
     */
    public static VerificationResult allowed(PlayerIdentity identity) {
        return new VerificationResult(true, null, identity);
    }

    /**
     * Creates a denied result.
     *
     * @param kickMessage the reason for denial
     * @return a failed VerificationResult
     */
    public static VerificationResult denied(Component kickMessage) {
        return new VerificationResult(false, kickMessage, null);
    }
}
