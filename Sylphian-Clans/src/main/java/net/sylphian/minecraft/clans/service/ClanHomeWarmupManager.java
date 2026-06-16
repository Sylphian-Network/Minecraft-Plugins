package net.sylphian.minecraft.clans.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending clan home teleport warmups.
 *
 * <p>A warmup shows a live action-bar countdown. Any block-level movement
 * cancels it immediately via {@link PlayerMoveEvent}. Disconnecting also
 * cancels the warmup to prevent task leaks.</p>
 */
public class ClanHomeWarmupManager implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final int warmupSeconds;

    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();

    /**
     * @param plugin        the owning plugin, used to schedule tasks
     * @param warmupSeconds how long the player must stand still before teleporting;
     *                      zero or negative means teleport instantly
     */
    public ClanHomeWarmupManager(JavaPlugin plugin, int warmupSeconds) {
        this.plugin = plugin;
        this.warmupSeconds = warmupSeconds;
    }

    /**
     * Begins a warmup countdown for the player, cancelling any prior warmup first.
     * Safe to call from off the main thread.
     *
     * @param player the player requesting the teleport
     * @param dest   the destination location
     */
    public void start(Player player, Location dest) {
        cancel(player.getUniqueId());

        if (warmupSeconds <= 0) {
            player.teleportAsync(dest).thenAccept(success -> {
                if (success) player.sendMessage(MINI.deserialize("<green>Teleported to clan home."));
                else         player.sendMessage(Component.text("Teleport failed.", NamedTextColor.RED));
            });
            return;
        }

        UUID uuid = player.getUniqueId();
        int[] remaining = {warmupSeconds};

        updateActionBar(player, remaining[0]);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                BukkitTask self = pending.remove(uuid);
                if (self != null) self.cancel();
                player.sendActionBar(Component.empty());
                player.teleportAsync(dest).thenAccept(success -> {
                    if (success) player.sendMessage(MINI.deserialize("<green>Teleported to clan home."));
                    else         player.sendMessage(Component.text("Teleport failed.", NamedTextColor.RED));
                });
            } else {
                updateActionBar(player, remaining[0]);
            }
        }, 20L, 20L);

        pending.put(uuid, task);
    }

    /**
     * Cancels the pending warmup for a player. No-op if none exists.
     * Safe to call from any thread.
     *
     * @param uuid the player's UUID
     */
    public void cancel(UUID uuid) {
        BukkitTask task = pending.remove(uuid);
        if (task != null) task.cancel();
    }

    /**
     * @param uuid the player's UUID
     * @return {@code true} if this player has a pending warmup
     */
    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPending(player.getUniqueId())) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        cancel(player.getUniqueId());
        player.sendActionBar(MINI.deserialize("<red>Teleport cancelled."));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    private void updateActionBar(Player player, int seconds) {
        String unit = seconds == 1 ? "second" : "seconds";
        player.sendActionBar(MINI.deserialize(
                "<yellow>Teleporting home in <white>" + seconds + " " + unit + "<yellow>..."));
    }
}
