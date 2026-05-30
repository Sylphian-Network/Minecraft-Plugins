package net.sylphian.verify.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.sylphian.verify.paper.api.model.VerificationReason;
import net.sylphian.verify.paper.api.model.VerificationResponse;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.verify.paper.model.PlayerIdentity;
import org.jspecify.annotations.NonNull;

/**
 * Utility class for constructing user-facing Adventure {@link Component} messages.
 * Centralizes the formatting logic for kick messages, cooldown notifications, and success alerts.
 */
public class MessageUtils {
    /**
     * Builds a kick message based on the API verification response.
     *
     * @param response the API response
     * @param config   the plugin configuration for localized messages
     * @return a formatted Adventure Component
     */
    public static Component buildKickMessage(VerificationResponse response, FileConfiguration config) {
        VerificationReason reason = response.getReason();

        // Select the appropriate config path based on the failure reason
        String path = "messages.";
        if (reason == VerificationReason.UUID_NOT_LINKED) {
            path += "not_linked";
        } else if (reason == VerificationReason.ACCOUNT_NOT_CONFIRMED) {
            path += "not_confirmed";
        } else {
            path += "api_error";
        }

        String displayReason = config.getString(path);
        if (displayReason == null) {
            displayReason = "Verification failed: " + reason;
        }

        return getMessage(response, displayReason);
    }

    /**
     * Internal helper to format the base kick message and append the passcode if present.
     *
     * @param response      the verification response
     * @param displayReason the base reason string
     * @return a formatted Component
     */
    private static @NonNull Component getMessage(VerificationResponse response, String displayReason) {
        Component message = Component.text(displayReason, NamedTextColor.RED, TextDecoration.BOLD);

        // If the API provided a passcode (for first-time linking), show it prominently
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
     * Builds a message informing the player they are on cooldown due to excessive attempts.
     *
     * @param expiryMillis the timestamp when the cooldown expires
     * @param config       the plugin configuration
     * @return a formatted Component with the remaining time
     */
    public static Component buildCooldownMessage(long expiryMillis, FileConfiguration config) {
        long remainingMillis = expiryMillis - System.currentTimeMillis();
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)) % 60;

        String timeLeft = String.format("%d:%02d", minutes, seconds);

        String displayReason = config.getString("messages.brute_force_blocked", "Too many failed attempts. Please try again in " + timeLeft + ".");

        // Replace the {time} placeholder if it exists in the config string
        if (displayReason.contains("{time}")) {
            displayReason = displayReason.replace("{time}", timeLeft);
        }

        return Component.text(displayReason, NamedTextColor.RED);
    }

    /**
     * Builds a generic error message for API failures.
     *
     * @param config the plugin configuration
     * @return a formatted Component
     */
    public static Component buildErrorMessage(FileConfiguration config) {
        String displayReason = config.getString("messages.api_error", "An error occurred while verifying your account. Please try again later.");
        return Component.text(displayReason, NamedTextColor.RED);
    }

    /**
     * Builds a message for when a previously verified session fails re-verification.
     *
     * @param config the plugin configuration
     * @return a formatted Component
     */
    public static Component buildReverificationFailureMessage(FileConfiguration config) {
        String displayReason = config.getString("messages.re_verification_failed", "Your account is no longer verified. Please ensure your account is linked at sylphian.net.");
        return Component.text(displayReason, NamedTextColor.RED, TextDecoration.BOLD);
    }

    /**
     * Builds a success message to be shown to the player (and potentially staff).
     *
     * @param identity the verified player identity
     * @return a formatted Component with the forum username
     */
    public static Component buildVerificationMessage(PlayerIdentity identity) {
        return Component.text("Verification successful! ", NamedTextColor.GREEN)
                .append(Component.text("Connected as ", NamedTextColor.GRAY))
                .append(Component.text(identity.forumUsername(), NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY));
    }
}
