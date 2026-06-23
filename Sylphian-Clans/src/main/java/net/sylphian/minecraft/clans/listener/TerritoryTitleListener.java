package net.sylphian.minecraft.clans.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.sylphian.minecraft.clans.event.ClanTerritoryEnterEvent;
import net.sylphian.minecraft.clans.event.ClanTerritoryExitEvent;
import net.sylphian.minecraft.clans.model.Clan;
import net.sylphian.minecraft.clans.service.ClanService;
import net.sylphian.minecraft.clans.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.UUID;

/**
 * Shows a fading title when a player enters a clan's territory, and a "Wilderness" title
 * when they step out into unclaimed land. A pure consumer of
 * {@link ClanTerritoryEnterEvent} and {@link ClanTerritoryExitEvent}; it does no
 * movement tracking of its own.
 */
public class TerritoryTitleListener implements Listener {

    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500));

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final TerritoryService territoryService;
    private final ClanService clanService;

    /**
     * @param territoryService used to check whether the destination chunk is unclaimed
     * @param clanService      used to resolve the entered clan's name and the viewer's membership
     */
    public TerritoryTitleListener(TerritoryService territoryService, ClanService clanService) {
        this.territoryService = territoryService;
        this.clanService = clanService;
    }

    @EventHandler
    public void onEnter(ClanTerritoryEnterEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;

        UUID clanId = event.getClanId();
        Clan cached = clanService.getClanByPlayerCached(player.getUniqueId()).orElse(null);
        if (cached != null && cached.clanId().equals(clanId)) {
            showClanTitle(player, cached, true);  // own territory: no DB hit needed
            return;
        }
        clanService.getClanById(clanId).thenAccept(opt -> opt.ifPresent(c -> showClanTitle(player, c, false)));
    }

    @EventHandler
    public void onExit(ClanTerritoryExitEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;

        // Only announce wilderness if the player is now standing in unclaimed land. If they
        // stepped straight into another clan's territory, the enter event announces that instead.
        Chunk chunk = player.getLocation().getChunk();
        if (territoryService.getClaimingClan(player.getWorld().getName(), chunk.getX(), chunk.getZ()).isEmpty()) {
            player.showTitle(Title.title(
                    Component.text("Wilderness", NamedTextColor.GRAY),
                    Component.empty(),
                    TIMES));
        }
    }

    private void showClanTitle(Player player, Clan clan, boolean own) {
        NamedTextColor color = own ? NamedTextColor.GREEN : NamedTextColor.RED;
        Component subtitle = (clan.motd() != null && !clan.motd().isBlank())
                ? MINI.deserialize(clan.motd())
                : Component.empty();
        player.showTitle(Title.title(
                Component.text(clan.name(), color),
                subtitle,
                TIMES));
    }
}
