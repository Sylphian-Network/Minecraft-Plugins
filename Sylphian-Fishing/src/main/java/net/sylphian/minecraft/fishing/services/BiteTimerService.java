package net.sylphian.minecraft.fishing.services;

import net.sylphian.minecraft.fishing.services.bait.BaitZone;
import net.sylphian.minecraft.fishing.config.BiteTimerConfig;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.fish.WeatherCondition;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Service responsible for overriding the vanilla fishing bite timer.
 *
 * <p>Calculates a custom wait time based on a pre-rolled rarity estimate
 * and the current weather condition, then applies it directly to the
 * {@link FishHook} entity. Rarer fish take longer to bite; storms speed
 * up the process.</p>
 *
 * <p>The pre-rolled rarity is used only for timing — the actual catch
 * rolls independently when the fish bites.</p>
 */
public class BiteTimerService {

    private ConfigLoader config;
    private final LootService lootService;
    private final BaitZoneService baitZoneService;
    private final Logger logger;
    private final Random random = new Random();

    /**
     * Constructs a new BiteTimerService.
     *
     * @param config      the config loader providing bite timer settings
     * @param lootService the loot service used to peek at a likely rarity
     * @param baitZoneService the service used to check for active bait zones at the hook location
     * @param logger      the logger for debug output
     */
    public BiteTimerService(ConfigLoader config, LootService lootService, BaitZoneService baitZoneService, Logger logger) {
        this.config = config;
        this.lootService = lootService;
        this.baitZoneService = baitZoneService;
        this.logger = logger;
    }

    /**
     * Calculates and applies a custom bite delay to the given fishing hook.
     * Called on {@link org.bukkit.event.player.PlayerFishEvent.State#FISHING}.
     *
     * @param hook   the fishing hook to apply the delay to
     * @param player the player who cast the hook
     */
    public void applyBiteTimer(FishHook hook, Player player) {
        WeatherCondition weather = WeatherCondition.from(player.getWorld());
        Rarity preRolledRarity = lootService.peekRarity(weather);

        BiteTimerConfig timerConfig = config.getBiteTimerConfig();
        int delay = timerConfig.calculate(preRolledRarity, weather, random);

        BaitZone zone = baitZoneService.getZoneAt(hook.getLocation());
        if (zone != null) {
            delay = (int) (delay * zone.config().biteTimerMultiplier());
            delay = Math.max(20, delay);
        }

        hook.setMinWaitTime(delay);
        hook.setMaxWaitTime(delay);

        logger.fine("Bite timer set to " + delay + " ticks for "
                + preRolledRarity.getId() + " rarity in " + weather.name()
                + (zone != null ? " (bait: " + zone.config().id() + ")" : ""));
    }

    /**
     * Reloads the service with updated configuration.
     *
     * @param config the new configuration loader
     */
    public void reload(ConfigLoader config) {
        this.config = config;
    }
}