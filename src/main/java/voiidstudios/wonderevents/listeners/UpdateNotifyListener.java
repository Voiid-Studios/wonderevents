package voiidstudios.wonderevents.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.update.UpdateChecker;

import java.util.HashMap;
import java.util.Map;

public final class UpdateNotifyListener implements Listener {
    private final WEBootstrap plugin;
    private final PluginContext context;

    public UpdateNotifyListener(WEBootstrap plugin, PluginContext context) {
        this.plugin = plugin;
        this.context = context;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String latest = plugin.getFoundNewVersion();
        if (latest == null) {
            return;
        }

        if (!context.getConfigManager().isUpdateNotification()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("wonderevents.updatenotify")) {
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%LATEST%", latest);
        placeholders.put("%CURRENT%", plugin.version);
        placeholders.put("%UPDATELINK%", UpdateChecker.RELEASES_PAGE_URL);

        context.getMessagesManager().sendList(player, "system.update.available", placeholders);
    }
}
