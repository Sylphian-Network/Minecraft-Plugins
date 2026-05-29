package net.sylphian.verify.paper.model;

import net.kyori.adventure.text.Component;

public record VerificationResult(boolean allowed, Component kickMessage, PlayerIdentity identity) {

    public static VerificationResult allowed(PlayerIdentity identity) {
        return new VerificationResult(true, null, identity);
    }

    public static VerificationResult denied(Component kickMessage, PlayerIdentity identity) {
        return new VerificationResult(false, kickMessage, identity);
    }
}
