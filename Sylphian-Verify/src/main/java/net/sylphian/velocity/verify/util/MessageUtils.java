package net.sylphian.velocity.verify.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.sylphian.velocity.verify.api.model.VerificationReason;
import net.sylphian.velocity.verify.api.model.VerificationResponse;
import java.util.Map;
import net.sylphian.velocity.verify.model.PlayerIdentity;
import org.jspecify.annotations.NonNull;

/**
 * Utility class for building user-facing Adventure components on Velocity.
 * Translates verification results and cooldowns into formatted text messages.
 */
public class MessageUtils {
    /**
     * Builds a kick message for a failed verification attempt.
     *
     * @param response the verification response from the API
     * @param config   the plugin configuration map
     * @return a formatted Adventure Component
     */
    public static Component buildKickMessage(VerificationResponse response, Map<String, Object> config) {
        VerificationReason reason = response.getReason();

        if (reason == null) {
            reason = VerificationReason.ACCOUNT_NOT_CONFIRMED;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) config.get("messages");
        String path = "api_error";
        if (reason == VerificationReason.UUID_NOT_LINKED) {
            path = "not_linked";
        } else if (reason == VerificationReason.ACCOUNT_NOT_CONFIRMED) {
            path = "not_confirmed";
        }

        String displayReason = messages != null ? messages.get(path) : null;
        if (displayReason == null) {
            displayReason = "Verification failed: " + reason.name();
        }

        return getMessage(response, displayReason);
    }

    /**
     * Internal helper to append passcode information to a base message.
     *
     * @param response      the verification response
     * @param displayReason the base reason string
     * @return a formatted Component
     */
    private static @NonNull Component getMessage(VerificationResponse response, String displayReason) {
        Component message = Component.text(displayReason, NamedTextColor.RED, TextDecoration.BOLD);

        if (response.getPasscode() != null && !response.getPasscode().isEmpty()) {
            message = message.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Your verification passcode is: ", NamedTextColor.YELLOW))
                    .append(Component.text(response.getPasscode(), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("Please enter this code on the website to link your account.", NamedTextColor.GRAY));
        }
        return message;
    }

    /**
     * Builds a message for players who are currently on login cooldown.
     *
     * @param expiryMillis the cooldown expiration timestamp
     * @param config       the plugin configuration map
     * @return a formatted Component with remaining time
     */
    public static Component buildCooldownMessage(long expiryMillis, Map<String, Object> config) {
        long remainingMillis = expiryMillis - System.currentTimeMillis();
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)) % 60;

        String timeLeft = String.format("%d:%02d", minutes, seconds);

        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) config.get("messages");
        String displayReason = messages != null ? messages.get("brute_force_blocked") : null;
        if (displayReason == null) {
            displayReason = "Too many failed attempts. Please try again in " + timeLeft + ".";
        }

        if (displayReason.contains("{time}")) {
            displayReason = displayReason.replace("{time}", timeLeft);
        }

        return Component.text(displayReason, NamedTextColor.RED);
    }

    /**
     * Builds a generic technical error message.
     *
     * @param config the plugin configuration map
     * @return a formatted Component
     */
    public static Component buildErrorMessage(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) config.get("messages");
        String displayReason = messages != null ? messages.get("api_error") : "An error occurred while verifying your account. Please try again later.";
        return Component.text(displayReason, NamedTextColor.RED);
    }

    /**
     * Builds a message for players who fail the background re-verification task.
     *
     * @param config the plugin configuration map
     * @return a formatted Component
     */
    public static Component buildReverificationFailureMessage(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) config.get("messages");
        String displayReason = messages != null ? messages.get("re_verification_failed") : "Your account is no longer verified. Please ensure your account is linked at sylphian.net.";
        return Component.text(displayReason, NamedTextColor.RED, TextDecoration.BOLD);
    }

    /**
     * Builds a success message shown when a player is verified.
     *
     * @param identity the verified identity
     * @return a formatted Component
     */
    public static Component buildVerificationMessage(PlayerIdentity identity) {
        return Component.text("Verification successful! ", NamedTextColor.GREEN)
                .append(Component.text("Connected as ", NamedTextColor.GRAY))
                .append(Component.text(identity.forumUsername(), NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY));
    }
}
