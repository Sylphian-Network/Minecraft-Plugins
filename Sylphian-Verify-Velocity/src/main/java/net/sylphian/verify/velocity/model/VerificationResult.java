package net.sylphian.verify.velocity.model;

import net.kyori.adventure.text.Component;

public record VerificationResult(boolean allowed, Component kickMessage, PlayerIdentity identity) {

    public static VerificationResult allowed(PlayerIdentity identity) {
        return new VerificationResult(true, null, identity);
    }

    public static VerificationResult denied(Component kickMessage) {
        return new VerificationResult(false, kickMessage, null);
    }
}
