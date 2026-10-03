package voiidstudios.wonderevents.core.platform;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import voiidstudios.tsunamilib.TsunamiLib;
import voiidstudios.tsunamilib.libs.adventure.text.Component;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.utils.TextUtils;
import voiidstudios.wonderevents.utils.UniversalFormatter;

public class PaperPlatformAdapter implements PlatformAdapter {
    private final Plugin plugin;
    private final UniversalFormatter formatter;
    private boolean warnedSendFallback;

    public PaperPlatformAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.formatter = new UniversalFormatter(plugin);
    }

    public static boolean isAvailable() {
        return hasClass("io.papermc.paper.configuration.Configuration") || hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    public String getName() {
        return supportsAdventure() ? "Paper/Fork + Adventure" : "Paper/Fork";
    }

    public boolean isPaper() {
        return true;
    }

    public boolean supportsAdventure() {
        return TsunamiLib.isAvailable() && TsunamiLib.getAPI().getAdventureManager().isReady();
    }

    public void sendMessage(CommandSender sender, String message) {
        Object formatted = formatter.format(message);
        if (formatted instanceof String) {
            sender.sendMessage((String) formatted);
            return;
        }

        if (!supportsAdventure() || !(formatted instanceof Component)) {
            sender.sendMessage(TextUtils.toLegacy(message));
            return;
        }

        try {
            TsunamiLib.getAPI().getAdventureManager().sender(sender).sendMessage((Component) formatted);
        } catch (RuntimeException | LinkageError exception) {
            warnSendFallback("Could not send the Adventure component", exception);
            sender.sendMessage(TextUtils.toLegacy(message));
        }
    }

    private void warnSendFallback(String message, Throwable throwable) {
        if (warnedSendFallback) {
            return;
        }

        warnedSendFallback = true;
        String warning = message + ", so falling back to legacy mode: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

        if (plugin instanceof WEBootstrap && ((WEBootstrap) plugin).getYALogger() != null) {
            ((WEBootstrap) plugin).getYALogger().warning(warning);
        } else {
            plugin.getLogger().warning(warning);
        }
    }
}
