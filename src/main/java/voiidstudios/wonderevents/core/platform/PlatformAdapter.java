package voiidstudios.wonderevents.core.platform;

import org.bukkit.command.CommandSender;

public interface PlatformAdapter {
    String getName();

    boolean isPaper();

    boolean supportsAdventure();

    void sendMessage(CommandSender sender, String message);
}
