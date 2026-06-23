package net.sylphian.minecraft.economy.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sylphian.minecraft.economy.api.EconomyAPI;
import net.sylphian.minecraft.economy.event.PlayerBalanceChangeEvent;
import net.sylphian.minecraft.economy.util.MoneyFormat;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exposes economy data as PlaceholderAPI placeholders.
 *
 * <p>Supported placeholders:
 * <ul>
 *   <li>{@code %sylphian_economy_balance%} — the player's formatted balance</li>
 * </ul>
 *
 * <p>Balance is cached on join and invalidated on quit, updated by
 * {@link PlayerBalanceChangeEvent} so the sidebar never polls the database.</p>
 */
public final class EconomyPlaceholderExpansion extends PlaceholderExpansion implements Listener {

    private final EconomyAPI economy;
    private final Map<UUID, BigDecimal> cache = new ConcurrentHashMap<>();

    public EconomyPlaceholderExpansion(EconomyAPI economy) {
        this.economy = economy;
    }

    @Override public String getIdentifier() { return "sylphian_economy"; }
    @Override public String getAuthor() { return "QuackieMackie"; }
    @Override public String getVersion() { return "1.0.0"; }

    /** Keep this expansion registered across PlaceholderAPI reloads. */
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return switch (params) {
            case "balance" -> {
                BigDecimal bal = cache.get(player.getUniqueId());
                yield bal != null ? MoneyFormat.format(bal) : "";
            }
            default -> null;
        };
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    /** Refreshes the cached balance when the economy announces a change. */
    @EventHandler
    public void onBalanceChange(PlayerBalanceChangeEvent event) {
        refresh(event.getPlayerId());
    }

    private void refresh(UUID uuid) {
        economy.getBalance(uuid).thenAccept(balance -> cache.put(uuid, balance));
    }
}