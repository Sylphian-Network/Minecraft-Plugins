package net.sylphian.minecraft.clans.api;

import org.jspecify.annotations.Nullable;

/**
 * Static access point to the clan API.
 *
 * <p>Other plugins should guard access with {@link #isAvailable()} when
 * Sylphian-Clans is declared as an optional dependency.</p>
 */
public final class ClanProvider {

    private static @Nullable ClanAPI api;

    private ClanProvider() {}

    /**
     * Registers the active clan implementation. Called once by Sylphian-Clans on enable.
     *
     * @param impl the implementation to expose
     */
    public static void register(ClanAPI impl) {
        api = impl;
    }

    /**
     * Clears the registered implementation. Called by Sylphian-Clans on disable.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * @return {@code true} if Sylphian-Clans is loaded and the API is available
     */
    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Returns the active clan API implementation.
     *
     * @return the registered {@link ClanAPI}
     * @throws IllegalStateException if Sylphian-Clans is not loaded
     */
    public static ClanAPI get() {
        if (api == null) {
            throw new IllegalStateException(
                    "Sylphian-Clans is not loaded; guard with ClanProvider.isAvailable() if it is optional.");
        }
        return api;
    }
}
