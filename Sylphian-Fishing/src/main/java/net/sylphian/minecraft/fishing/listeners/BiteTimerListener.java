package net.sylphian.minecraft.fishing.listeners;

import net.sylphian.minecraft.fishing.config.BiteTimerConfig;
import net.sylphian.minecraft.fishing.config.ConfigLoader;
import net.sylphian.minecraft.fishing.fish.Rarity;
import net.sylphian.minecraft.fishing.loot.LootManager;
import net.sylphian.minecraft.fishing.weather.WeatherCondition;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * Listener responsible for overriding the vanilla fishing bite timer.
 *
 * <p>Intercepts the bobber throw event and sets a custom wait time
 * on the {@link FishHook} entity based on the configured rarity modifiers
 * and current weather condition. This replaces Minecraft's default
 * random 5–30 second wait with a contextual delay — rarer fish take
 * longer to bite, while storms speed up the process.</p>
 *
 * <p>Uses the Paper {@link FishHook} API to set min/max lure times
 * directly on the hook entity rather than packet manipulation,
 * which is simpler and more reliable for this use case.</p>
 */
public class BiteTimerListener implements Listener {

    private final ConfigLoader config;
    private final LootManager lootManager;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    /**
     * Constructs a new BiteTimerListener.
     *
     * @param config      the config loader providing bite timer settings
     * @param lootManager the loot manager used to peek at likely rarity
     * @param plugin      the plugin instance for scheduling tasks
     */
    public BiteTimerListener(ConfigLoader config, LootManager lootManager, JavaPlugin plugin) {
        this.config = config;
        this.lootManager = lootManager;
        this.plugin = plugin;
    }

    /**
     * Handles the bobber throw event.
     * Calculates and applies a custom bite delay to the fishing hook
     * based on the current weather and a pre-rolled rarity estimate.
     *
     * @param event the fishing event
     */
    @EventHandler
    public void onBobberThrow(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) return;
        FishHook hook = event.getHook();

        Player player = event.getPlayer();
        WeatherCondition weather = WeatherCondition.from(player.getWorld());

        // Pre-roll a rarity to determine the wait time
        // This doesn't affect the actual catch — LootManager rolls again on catch
        Rarity preRolledRarity = lootManager.peekRarity(weather);

        BiteTimerConfig timerConfig = config.getBiteTimerConfig();
        int delay = timerConfig.calculate(preRolledRarity, weather, random);

        hook.setMinWaitTime(delay);
        hook.setMaxWaitTime(delay);

        plugin.getLogger().fine("Bite timer set to " + delay + " ticks for "
                + preRolledRarity.getId() + " rarity in " + weather.name());
    }
}