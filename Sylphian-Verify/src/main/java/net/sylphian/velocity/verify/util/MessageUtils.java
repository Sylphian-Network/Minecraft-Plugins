package net.sylphian.velocity.verify.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sylphian.velocity.verify.api.model.VerificationReason;
import net.sylphian.velocity.verify.api.model.VerificationResponse;
import net.sylphian.velocity.verify.model.PlayerIdentity;

import java.util.Map;

/**
 * Utility class for building user-facing Adventure components on Velocity.
 * Translates verification results and cooldowns into formatted text messages.
 */
public class MessageUtils {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

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

        String path = switch (reason) {
            case UUID_NOT_LINKED -> "not_linked";
            case ACCOUNT_NOT_CONFIRMED -> "not_confirmed";
            default -> "api_error";
        };

        String template = messages(config).get(path);
        Component message = template != null
                ? MINI.deserialize(template)
                : MINI.deserialize("<red><bold>Verification failed: <reason>",
                        Placeholder.unparsed("reason", reason.name()));

        return message.append(passcodeSuffix(response));
    }

    private static Component passcodeSuffix(VerificationResponse response) {
        if (response.getPasscode() == null || response.getPasscode().isEmpty()) {
            return Component.empty();
        }
        return MINI.deserialize("""
                <newline><newline><yellow>Your verification passcode is: <white><bold><passcode></bold></white>
                <gray>Please enter this code on the website to link your account.""",
                Placeholder.unparsed("passcode", response.getPasscode()));
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

        String template = messages(config).getOrDefault("brute_force_blocked",
                "<red>Too many failed attempts. Please try again in <time>.");

        return MINI.deserialize(template, Placeholder.unparsed("time", timeLeft));
    }

    /**
     * Builds a generic technical error message.
     *
     * @param config the plugin configuration map
     * @return a formatted Component
     */
    public static Component buildErrorMessage(Map<String, Object> config) {
        String template = messages(config).getOrDefault("api_error",
                "<red>An error occurred while verifying your account. Please try again later.");
        return MINI.deserialize(template);
    }

    /**
     * Builds a message for players who fail the background re-verification task.
     *
     * @param config the plugin configuration map
     * @return a formatted Component
     */
    public static Component buildReverificationFailureMessage(Map<String, Object> config) {
        String template = messages(config).getOrDefault("re_verification_failed",
                "<red><bold>Your account is no longer verified. Please ensure your account is linked at sylphian.net.");
        return MINI.deserialize(template);
    }

    /**
     * Builds a success message shown when a player is verified.
     *
     * @param identity the verified identity
     * @return a formatted Component
     */
    public static Component buildVerificationMessage(PlayerIdentity identity) {
        return MINI.deserialize(
                "<green>Verification successful! <gray>Connected as <aqua><bold><username></bold></aqua><gray>.",
                Placeholder.unparsed("username", identity.forumUsername()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> messages(Map<String, Object> config) {
        Map<String, String> messages = (Map<String, String>) config.get("messages");
        return messages != null ? messages : Map.of();
    }
}
