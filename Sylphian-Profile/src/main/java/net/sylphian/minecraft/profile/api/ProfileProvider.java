package net.sylphian.minecraft.profile.api;

import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Static access point to the profile API.
 *
 * <p>Consumers that optionally depend on Sylphian-Profile should guard access
 * with {@link #isAvailable()} before calling {@link #get()}.</p>
 */
public final class ProfileProvider {

    private static @Nullable ProfileAPI api;

    private ProfileProvider() {}

    /**
     * Registers the active profile implementation. Called once by Sylphian-Profile on enable.
     *
     * @param impl the implementation to expose
     */
    public static void register(ProfileAPI impl) {
        api = impl;
    }

    /**
     * Clears the registered implementation. Called by Sylphian-Profile on disable.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * @return {@code true} if a profile implementation is currently registered
     */
    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Returns the active profile implementation.
     *
     * @return the registered {@link ProfileAPI}
     * @throws IllegalStateException if Sylphian-Profile is not loaded
     */
    public static ProfileAPI get() {
        if (api == null) {
            throw new IllegalStateException(
                    "Sylphian-Profile is not loaded; guard with ProfileProvider.isAvailable() if it is optional.");
        }
        return api;
    }

    /**
     * Public interface for cross-module profile operations.
     */
    public interface ProfileAPI {

        /**
         * Ensures a player row exists in {@code sylphian_profile_players}.
         * If the row already exists, updates {@code xf_user_id}, {@code mc_username},
         * and {@code forum_username} to match the supplied values.
         * If the row does not exist, inserts it with sensible defaults.
         *
         * <p>Executes asynchronously on the database executor. The returned future
         * completes with no value when the upsert is done.</p>
         *
         * @param uuid          the player's Mojang UUID
         * @param xfUserId      the linked XenForo user ID, or {@code null} if unknown
         * @param mcUsername    the player's current Minecraft username
         * @param forumUsername the player's XenForo username, or {@code null} if unknown
         * @return a future that completes when the operation is done
         */
        CompletableFuture<Void> ensurePlayerExists(UUID uuid, @Nullable Integer xfUserId,
                                                   String mcUsername, @Nullable String forumUsername);
    }
}
