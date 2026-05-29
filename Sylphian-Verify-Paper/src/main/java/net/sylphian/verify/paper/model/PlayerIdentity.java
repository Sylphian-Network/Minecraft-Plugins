package net.sylphian.verify.paper.model;

import net.sylphian.verify.paper.api.model.VerificationResponse;

import java.util.UUID;

public record PlayerIdentity(UUID uuid, int xfUserId, String forumUsername, String mcUsername) {
    public static final String CHANNEL = "sylphian:verify";

    public static PlayerIdentity from(VerificationResponse response, UUID uuid) {
        return new PlayerIdentity(
                uuid,
                response.getXfUserId(),
                response.getForumUsername(),
                response.getMcUsername()
        );
    }
}
