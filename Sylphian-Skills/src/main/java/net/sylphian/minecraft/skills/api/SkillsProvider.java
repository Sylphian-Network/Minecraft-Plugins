package net.sylphian.minecraft.skills.api;

import org.jspecify.annotations.Nullable;

/**
 * Static access point to the skills API.
 *
 * <p>Other plugins should guard access with {@link #isAvailable()} when
 * Sylphian-Skills is declared as an optional dependency.</p>
 */
public final class SkillsProvider {

    private static @Nullable SkillsAPI api;

    private SkillsProvider() {}

    /**
     * Registers the active implementation. Called once by Sylphian-Skills on enable.
     *
     * @param impl the implementation to expose
     */
    public static void register(SkillsAPI impl) {
        api = impl;
    }

    /**
     * Clears the registered implementation. Called by Sylphian-Skills on disable.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * @return {@code true} if Sylphian-Skills is loaded and the API is available
     */
    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Returns the active skills API.
     *
     * @return the registered {@link SkillsAPI}
     * @throws IllegalStateException if Sylphian-Skills is not loaded
     */
    public static SkillsAPI get() {
        if (api == null) {
            throw new IllegalStateException(
                    "Sylphian-Skills is not loaded; guard with SkillsProvider.isAvailable() if it is optional.");
        }
        return api;
    }
}
