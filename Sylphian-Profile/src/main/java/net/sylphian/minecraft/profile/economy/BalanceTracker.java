package net.sylphian.minecraft.profile.economy;

import net.sylphian.minecraft.economy.api.EconomyProvider;
import net.sylphian.minecraft.economy.event.EconomyConfigReloadEvent;
import net.sylphian.minecraft.economy.event.PlayerBalanceChangeEvent;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import net.sylphian.minecraft.profile.sidebar.BalanceSupplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds each online player's last-known balance for synchronous sidebar reads.
 *
 * <p>Updated by events, not polling: seeded on join, refreshed on
 * {@link PlayerBalanceChangeEvent}, and cleared on quit. Only instantiated and
 * registered when Sylphian-Economy is installed.</p>
 */
public class BalanceTracker implements BalanceSupplier, Listener {

    private final Map<UUID, String> snapshot = new ConcurrentHashMap<>();

    /** Seeds the player's balance once when they join. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer().getUniqueId());
    }

    /** Refreshes the snapshot when the economy announces a change for this server. */
    @EventHandler
    public void onBalanceChange(PlayerBalanceChangeEvent event) {
        refresh(event.getPlayerId());
    }

    /** Drops the snapshot when the player leaves. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        snapshot.remove(event.getPlayer().getUniqueId());
    }

    /** Re-formats every online player's balance when the currency symbol may have changed. */
    @EventHandler
    public void onConfigReload(EconomyConfigReloadEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player.getUniqueId());
        }
    }

    private void refresh(UUID uuid) {
        if (!EconomyProvider.isAvailable()) {
            return;
        }
        // Async read off the render path; the result lands in the thread-safe snapshot.
        EconomyProvider.get().getBalance(uuid)
                .thenAccept(balance -> snapshot.put(uuid, MoneyFormat.format(balance)));
    }

    @Override
    public @Nullable String formattedBalance(UUID uuid) {
        return snapshot.get(uuid);
    }
}
