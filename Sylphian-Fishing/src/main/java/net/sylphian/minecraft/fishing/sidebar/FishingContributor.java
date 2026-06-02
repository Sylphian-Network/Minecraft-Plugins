package net.sylphian.minecraft.fishing.sidebar;

import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import net.sylphian.minecraft.fishing.services.BaitZoneService;
import net.sylphian.minecraft.fishing.services.bait.BaitZone;
import net.sylphian.minecraft.scoreboard.api.AbstractSidebarContributor;
import net.sylphian.minecraft.scoreboard.api.SidebarLine;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contributes fishing session information to the player sidebar.
 *
 * <p>Lines are only shown while the player's hook is bobbing in water,
 * and include biome, weather, elapsed cast time, and any active bait zones
 * at the hook's location.</p>
 */
public class FishingContributor extends AbstractSidebarContributor {
    public static final int PRIORITY = 10;

    private final BaitZoneService baitZoneService;

    private final Map<UUID, Long> castTimes = new HashMap<>();
    private final Map<UUID, FishHook> activeHooks = new HashMap<>();

    /**
     * Constructs a new FishingContributor.
     *
     * @param baitZoneService the service used to query active bait zones
     */
    public FishingContributor(BaitZoneService baitZoneService) {
        super("sylphian-fishing", PRIORITY);
        this.baitZoneService = baitZoneService;
    }

    /**
     * Returns bait zone lines if the player's hook is bobbing in one or more zones.
     * Returns an empty list otherwise, hiding the section.
     *
     * @param player the player to get lines for
     * @return sidebar lines showing active bait zones, or empty
     */
    @Override
    public List<SidebarLine> getLinesFor(Player player) {
        FishHook hook = activeHooks.get(player.getUniqueId());
        if (hook == null || !hook.isValid()) return List.of();
        if (hook.getState() != FishHook.HookState.BOBBING) return List.of();

        World world = hook.getWorld();
        WeatherCondition weather = WeatherCondition.from(world);
        String biome = formatBiome(hook.getLocation().getBlock().getBiome().key().value());

        long elapsed = (System.currentTimeMillis() - castTimes.getOrDefault(player.getUniqueId(), System.currentTimeMillis())) / 1000;
        String time = String.format("%d:%02d", elapsed / 60, elapsed % 60);

        String weatherIcon = switch (weather) {
            case THUNDERSTORM -> "⛈ Thunderstorm";
            case RAIN ->         "🌧 Rain";
            case CLEAR ->        "☀ Clear";
        };

        List<SidebarLine> lines = new ArrayList<>();
        lines.add(new SidebarLine(MINI.deserialize("<gray>🎣 Fishing")));
        lines.add(new SidebarLine(MINI.deserialize("<dark_gray>Biome: <gray>" + biome)));
        lines.add(new SidebarLine(MINI.deserialize("<dark_gray>Weather: <gray>" + weatherIcon)));
        lines.add(new SidebarLine(MINI.deserialize("<dark_gray>Elapsed: <gray>" + time)));

        List<BaitZone> zones = baitZoneService.getZonesAt(hook.getLocation());
        if (!zones.isEmpty()) {
            lines.add(new SidebarLine(MINI.deserialize("<gray>Active Baits")));
            for (BaitZone zone : zones) {
                lines.add(new SidebarLine(MINI.deserialize("<dark_gray>• <gray>" + zone.config().displayName())));
            }
        }

        return lines;
    }

    /**
     * Converts a biome registry key (e.g. "lukewarm_ocean") to title-case display text.
     *
     * @param key the raw biome key value
     * @return formatted display name
     */
    private String formatBiome(String key) {
        String[] words = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /**
     * Registers the player's active fishing hook for zone tracking.
     *
     * @param uuid the player's UUID
     * @param hook the active fishing hook
     */
    public void trackHook(UUID uuid, FishHook hook) {
        activeHooks.put(uuid, hook);
        castTimes.put(uuid, System.currentTimeMillis());
    }

    /**
     * Removes the player's hook from tracking.
     *
     * @param uuid the player's UUID
     */
    public void clearHook(UUID uuid) {
        activeHooks.remove(uuid);
        castTimes.remove(uuid);
    }
}