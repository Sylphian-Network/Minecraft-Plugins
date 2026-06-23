package net.sylphian.minecraft.profile.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Delegates to PlaceholderAPI. Only instantiated when PlaceholderAPI is present. */
public final class PapiPlaceholderBridge implements PlaceholderResolver {

    @Override
    public String resolve(Player player, String placeholder) {
        return PlaceholderAPI.setPlaceholders(player, placeholder);
    }
}