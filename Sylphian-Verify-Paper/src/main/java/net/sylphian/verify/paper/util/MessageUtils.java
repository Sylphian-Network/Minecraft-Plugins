package net.sylphian.verify.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.sylphian.verify.paper.api.model.VerificationReason;
import net.sylphian.verify.paper.api.model.VerificationResponse;
import org.bukkit.configuration.file.FileConfiguration;
import net.sylphian.verify.paper.model.PlayerIdentity;
import org.jspecify.annotations.NonNull;

public class MessageUtils {
    public static Component buildKickMessage(VerificationResponse response, FileConfiguration config) {
        VerificationReason reason = response.getReason();

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

    public static Component buildCooldownMessage(long expiryMillis, FileConfiguration config) {
        long remainingMillis = expiryMillis - System.currentTimeMillis();
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)) % 60;

        String timeLeft = String.format("%d:%02d", minutes, seconds);

        String displayReason = config.getString("messages.brute_force_blocked", "Too many failed attempts. Please try again in " + timeLeft + ".");

        if (displayReason.contains("{time}")) {
            displayReason = displayReason.replace("{time}", timeLeft);
        }

        return Component.text(displayReason, NamedTextColor.RED);
    }

    public static Component buildErrorMessage(FileConfiguration config) {
        String displayReason = config.getString("messages.api_error", "An error occurred while verifying your account. Please try again later.");
        return Component.text(displayReason, NamedTextColor.RED);
    }

    public static Component buildReverificationFailureMessage(FileConfiguration config) {
        String displayReason = config.getString("messages.re_verification_failed", "Your account is no longer verified. Please ensure your account is linked at sylphian.net.");
        return Component.text(displayReason, NamedTextColor.RED, TextDecoration.BOLD);
    }

    public static Component buildVerificationMessage(PlayerIdentity identity) {
        return Component.text("Verification successful! ", NamedTextColor.GREEN)
                .append(Component.text("Connected as ", NamedTextColor.GRAY))
                .append(Component.text(identity.forumUsername(), NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY));
    }
}
