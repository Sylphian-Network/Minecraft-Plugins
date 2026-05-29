package net.sylphian.minecraft.profile.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.sylphian.minecraft.profile.utils.ProfileManager;
import net.sylphian.minecraft.profile.SylphianProfile;
import net.sylphian.minecraft.profile.UserProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {
    private final SylphianProfile plugin;
    private final ProfileManager profileManager;

    public ChatListener(SylphianProfile plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        UserProfile profile = profileManager.getProfile(uuid);
        if (profile == null) return;

        event.renderer(plugin.getVisualManager().getChatRenderer(player, profile));
    }
}
