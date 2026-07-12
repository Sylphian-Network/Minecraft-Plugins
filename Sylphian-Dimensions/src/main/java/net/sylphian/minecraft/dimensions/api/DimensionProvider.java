package net.sylphian.minecraft.dimensions.api;

import org.jspecify.annotations.Nullable;

/**
 * Static access point to the dimension API.
 *
 * <p>Other plugins should guard access with {@link #isAvailable()} when
 * Sylphian-Dimensions is declared as an optional dependency.</p>
 */
public final class DimensionProvider {

    private static @Nullable DimensionAPI api;

    private DimensionProvider() {}

    /**
     * Registers the active dimension implementation. Called once by Sylphian-Dimensions on enable.
     *
     * @param impl the implementation to expose
     */
    public static void register(DimensionAPI impl) {
        api = impl;
    }

    /**
     * Clears the registered implementation. Called by Sylphian-Dimensions on disable.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * @return {@code true} if Sylphian-Dimensions is loaded and the API is available
     */
    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Returns the active dimension API implementation.
     *
     * @return the registered {@link DimensionAPI}
     * @throws IllegalStateException if Sylphian-Dimensions is not loaded
     */
    public static DimensionAPI get() {
        if (api == null) {
            throw new IllegalStateException(
                    "Sylphian-Dimensions is not loaded; guard with DimensionProvider.isAvailable() if it is optional.");
        }
        return api;
    }
}
