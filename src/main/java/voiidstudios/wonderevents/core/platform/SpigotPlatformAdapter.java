package voiidstudios.wonderevents.core.platform;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import voiidstudios.wonderevents.utils.TextUtils;
import voiidstudios.wonderevents.utils.UniversalFormatter;

public class SpigotPlatformAdapter implements PlatformAdapter {
    private final UniversalFormatter formatter;

    public SpigotPlatformAdapter(Plugin plugin) {
        this.formatter = new UniversalFormatter(plugin);
    }

    public String getName() {
        return "Spigot/Bukkit";
    }

    public boolean isPaper() {
        return false;
    }

    public boolean supportsAdventure() {
        return false;
    }

    public void sendMessage(CommandSender sender, String message) {
        Object formatted = formatter.format(message);
        sender.sendMessage(formatted instanceof String ? (String) formatted : TextUtils.toLegacy(message));
    }
}
