package net.sylphian.minecraft.clans.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends a fading title to players when they cross chunk boundaries into
 * or out of claimed territory.
 */
public class TerritoryNotificationListener implements Listener {

    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500));

    private final TerritoryService territoryService;
    private final ClanService clanService;

    /**
     * @param territoryService used to resolve chunk ownership from the cache
     * @param clanService      used to fetch the clan name for owned chunks
     */
    public TerritoryNotificationListener(TerritoryService territoryService, ClanService clanService) {
        this.territoryService = territoryService;
        this.clanService = clanService;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        int fromX = event.getFrom().getChunk().getX();
        int fromZ = event.getFrom().getChunk().getZ();
        int toX   = event.getTo().getChunk().getX();
        int toZ   = event.getTo().getChunk().getZ();

        Optional<UUID> fromClan = territoryService.getClaimingClan(world, fromX, fromZ);
        Optional<UUID> toClan   = territoryService.getClaimingClan(world, toX, toZ);

        if (fromClan.equals(toClan)) return;

        if (toClan.isEmpty()) {
            player.showTitle(Title.title(
                    Component.text("Wilderness", NamedTextColor.GRAY),
                    Component.empty(),
                    TIMES));
            return;
        }

        UUID clanId = toClan.get();

        clanService.getClanByPlayerCached(player.getUniqueId())
                .filter(c -> c.clanId().equals(clanId))
                .ifPresentOrElse(c -> showTitle(player, c.name(), player.getUniqueId(), clanId),
                        () -> clanService.getClanById(clanId).thenAccept(opt ->
                                opt.ifPresent(c -> {
                                    boolean isOwn = c.members().stream()
                                            .anyMatch(m -> m.playerId().equals(player.getUniqueId()));
                                    showTitle(player, c.name(), isOwn);
                                })));
    }

    private void showTitle(Player player, String clanName, UUID playerUuid, UUID clanId) {
        clanService.getClanByPlayerCached(playerUuid)
                .ifPresentOrElse(
                        c -> showTitle(player, clanName, c.clanId().equals(clanId)),
                        () -> showTitle(player, clanName, false));
    }

    private void showTitle(Player player, String clanName, boolean isOwn) {
        NamedTextColor color = isOwn ? NamedTextColor.GREEN : NamedTextColor.RED;
        player.showTitle(Title.title(
                Component.text(clanName + "'s Territory", color),
                Component.empty(),
                TIMES));
    }
}
