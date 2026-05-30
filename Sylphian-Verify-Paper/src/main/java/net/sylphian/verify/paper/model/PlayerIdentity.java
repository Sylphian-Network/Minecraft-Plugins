package net.sylphian.verify.paper.model;

import net.sylphian.verify.paper.api.model.VerificationResponse;

import java.util.UUID;

/**
 * Represents a player's verified forum identity.
 * This object is used to transfer identity data between the API, the database,
 * and across the plugin messaging channel.
 *
 * @param uuid           the player's Mojang UUID
 * @param xfUserId       the linked XenForo user ID
 * @param forumUsername  the linked forum username
 * @param mcUsername     the player's Minecraft username as recorded on the forum
 */
public record PlayerIdentity(UUID uuid, int xfUserId, String forumUsername, String mcUsername) {
    /** The plugin messaging channel used for identity synchronization. */
    public static final String CHANNEL = "sylphian:verify";

    /**
     * Creates a PlayerIdentity from an API VerificationResponse.
     *
     * @param response the response from the verification API
     * @param uuid     the player's Mojang UUID
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
