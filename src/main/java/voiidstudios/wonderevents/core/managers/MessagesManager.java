package voiidstudios.wonderevents.core.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.commands.interfaces.BukkitCmdSender;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.utils.TextUtils;

import java.util.List;
import java.util.Map;

public final class MessagesManager {
    private final JavaPlugin plugin;
    private final YALogger logger;
    private final TranslationManager translations;

    public MessagesManager(JavaPlugin plugin, String language, YALogger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.translations = new TranslationManager(plugin, logger);
        this.translations.loadLanguage(language);
    }

    public void reload(String language) {
        translations.loadLanguage(language);
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = translations.formatKey(key, placeholders);
        if (raw == null) {
            return TextUtils.toLegacy("&cMissing message: " + key);
        }
        return color(raw);
    }

    public List<String> getList(String key) {
        return translations.getStringList(key);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, null);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public void sendPrefixed(CommandSender sender, String key) {
        sendPrefixed(sender, key, null);
    }

    public void sendPrefixed(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = get(key, placeholders);
        new BukkitCmdSender(sender).sendPrefixedMsg(msg);
    }

    public void sendList(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }
        for (String line : translations.getStringList(key)) {
            sender.sendMessage(color(translations.formatRaw(line, placeholders)));
        }
    }

    public void console(String key) {
        plugin.getServer().getConsoleSender().sendMessage(get(key));
    }

    public String color(String message) {
        return TextUtils.toLegacy(message == null ? "" : message);
    }

    public TranslationManager getTranslations() {
        return translations;
    }

    public YALogger getLogger() {
        return logger;
    }
}
