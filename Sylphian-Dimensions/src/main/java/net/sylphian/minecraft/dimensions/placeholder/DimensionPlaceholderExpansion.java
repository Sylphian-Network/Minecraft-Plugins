package net.sylphian.minecraft.dimensions.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sylphian.minecraft.dimensions.api.DimensionProvider;
import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

/**
 * Exposes dimension data as PlaceholderAPI placeholders.
 */
public final class DimensionPlaceholderExpansion extends PlaceholderExpansion {

    @Override public String getIdentifier() { return "sylphian-dimensions"; }
    @Override public String getAuthor() { return "QuackieMackie"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !DimensionProvider.isAvailable()) return "";
        if (params.equals("current")) {
            Optional<Dimension> dimension = DimensionProvider.get().getPlayerCurrentDimension(player.getUniqueId());
            return dimension.map(Dimension::name).orElse("");
        }
        return null;
    }
}
