package net.sylphian.velocity.verify.model;

import net.sylphian.velocity.verify.api.model.VerificationResponse;

import java.util.UUID;

/**
 * Represents a verified player identity on the Velocity proxy.
 *
 * @param uuid           the player's Mojang UUID
 * @param xfUserId       the linked XenForo user ID
 * @param forumUsername  the linked forum username
 * @param mcUsername     the player's Minecraft username as recorded on the forum
 */
public record PlayerIdentity(UUID uuid, int xfUserId, String forumUsername, String mcUsername) {
    /** The plugin messaging channel used to sync identity data to backend servers. */
    public static final String CHANNEL = "sylphian:verify";

    /**
     * @param response the API response data
     * @param uuid     the player's UUID
     * @return a new PlayerIdentity instance
     */
    public static PlayerIdentity from(VerificationResponse response, UUID uuid) {
        return new PlayerIdentity(
                uuid,
                response.getXfUserId(),
                response.getForumUsername(),
                response.getMcUsername()
        );
    }
}
