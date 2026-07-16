package net.sylphian.minecraft.profile.verify;

import java.util.UUID;

/**
 * A player's verified forum identity received from the Velocity proxy over the
 * plugin messaging channel.
 *
 * @param uuid          the player's Mojang UUID
 * @param xfUserId      the linked XenForo user ID
 * @param forumUsername the linked forum username
 * @param mcUsername    the player's Minecraft username as recorded on the forum
 */
public record PlayerIdentity(UUID uuid, int xfUserId, String forumUsername, String mcUsername) {
    /** The plugin messaging channel used for identity synchronization. */
    public static final String CHANNEL = "sylphian:verify";
}
