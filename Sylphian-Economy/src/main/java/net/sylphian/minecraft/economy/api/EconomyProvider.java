package net.sylphian.minecraft.economy.api;

import org.jspecify.annotations.Nullable;

/**
 * Static access point to the economy API.
 */
public final class EconomyProvider {

    private static @Nullable EconomyAPI api;

    private EconomyProvider() {
    }

    /**
     * Registers the active economy implementation. Called once by Sylphian-Economy.
     *
     * @param impl the implementation to expose network-wide
     */
    public static void register(EconomyAPI impl) {
        api = impl;
    }

    /**
     * Clears the registered implementation. Called by Sylphian-Economy on disable.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * @return {@code true} if an economy implementation is currently registered
     */
    public static boolean isAvailable() {
        return api != null;
    }

    /**
     * Returns the active economy implementation.
     *
     * @return the registered {@link EconomyAPI}
     * @throws IllegalStateException if Sylphian-Economy is not loaded
     */
    public static EconomyAPI get() {
        if (api == null) {
            throw new IllegalStateException(
                    "Sylphian-Economy is not loaded; guard with EconomyProvider.isAvailable() if it is optional.");
        }
        return api;
    }
}
