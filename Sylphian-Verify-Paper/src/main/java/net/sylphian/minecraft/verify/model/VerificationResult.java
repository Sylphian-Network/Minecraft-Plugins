package net.sylphian.minecraft.verify.model;

import net.kyori.adventure.text.Component;

/**
 * Represents the final result of a verification check within the plugin.
 * Combines the access decision, the visual kick message (if denied),
 * and the player's identity data.
 *
 * @param allowed     whether the player passed verification
 * @param kickMessage the component to display to the player if they are kicked
 * @param identity    the player's identity details, if available
 */
public record VerificationResult(boolean allowed, Component kickMessage, PlayerIdentity identity) {

    /**
     * Creates a successful verification result.
     *
     * @param identity the verified identity of the player
     * @return a successful VerificationResult
     */
    public static VerificationResult allowed(PlayerIdentity identity) {
        return new VerificationResult(true, null, identity);
    }

    /**
     * Creates a failed verification result.
     *
     * @param kickMessage the message explaining why the player was denied entry
     * @param identity    the identity data, if any was retrieved before failure
     * @return a failed VerificationResult
     */
    public static VerificationResult denied(Component kickMessage, PlayerIdentity identity) {
        return new VerificationResult(false, kickMessage, identity);
    }
}
