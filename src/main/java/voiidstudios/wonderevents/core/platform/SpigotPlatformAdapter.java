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

    @Override
    public String getName() {
        return "Spigot/Bukkit";
    }

    @Override
    public boolean isPaper() {
        return false;
    }

    @Override
    public boolean supportsAdventure() {
        return false;
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {
        Object formatted = formatter.format(message);
        sender.sendMessage(formatted instanceof String text ? text : TextUtils.toLegacy(message));
    }
}
