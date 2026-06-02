package net.sylphian.minecraft.scoreboard.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Loads and parses sidebar appearance configuration from {@code config.yml}.
 */
public class ScoreboardConfigLoader {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final FileConfiguration config;

    /**
     * Constructs a new ScoreboardConfigLoader.
     *
     * @param config the file configuration to load from
     */
    public ScoreboardConfigLoader(FileConfiguration config) {
        this.config = config;
    }

    /**
     * Parses and returns the scoreboard configuration.
     *
     * @return the parsed ScoreboardConfig
     */
    public ScoreboardConfig load() {
        Component title = MINI.deserialize(
                config.getString("sidebar.title", "<bold>Sylphian Network</bold>"));

        boolean showScores = config.getBoolean("sidebar.show-scores", false);

        int updateInterval = config.getInt("sidebar.update-interval", 10);

        List<Component> headerLines = Collections.emptyList();
        if (config.getBoolean("sidebar.header.enabled", true)) {
            headerLines = config.getStringList("sidebar.header.lines")
                    .stream()
                    .map(MINI::deserialize)
                    .toList();
        }

        List<Component> footerLines = Collections.emptyList();
        if (config.getBoolean("sidebar.footer.enabled", true)) {
            footerLines = config.getStringList("sidebar.footer.lines")
                    .stream()
                    .map(MINI::deserialize)
                    .toList();
        }

        return new ScoreboardConfig(title, showScores, updateInterval, headerLines, footerLines);
    }
}