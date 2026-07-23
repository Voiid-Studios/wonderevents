package voiidstudios.wonderevents.core.managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import voiidstudios.wonderevents.core.PluginContext;

import java.util.UUID;

public final class AdventureManager {
    private final PluginContext context;
    private BukkitAudiences audiences;

    public AdventureManager(PluginContext context) {
        this.context = context;
    }

    public void start() {
        if (audiences != null) {
            return;
        }

        try {
            audiences = BukkitAudiences.create(context.getPlugin());
        } catch (Exception exception) {
            if (context.getPlugin().getYALogger() != null) {
                context.getPlugin().getYALogger().severe("Could not start the Adventure bridge: " + exception.getMessage(), exception);
            }
        }
    }

    public void stop() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    public boolean isReady() {
        return audiences != null;
    }

    public BukkitAudiences audiences() {
        if (audiences == null) {
            throw new IllegalStateException("The WonderEvents Adventure bridge is not ready yet (plugin disabled?)");
        }
        return audiences;
    }

    public Audience player(Player player) {
        return audiences().player(player);
    }

    public Audience player(UUID playerId) {
        return audiences().player(playerId);
    }

    public Audience sender(CommandSender sender) {
        return audiences().sender(sender);
    }

    public Audience console() {
        return audiences().console();
    }

    public Audience players() {
        return audiences().players();
    }

    public Audience all() {
        return audiences().all();
    }

    public Component mini(String text) {
        return miniMessage().deserialize(text == null ? "" : text);
    }

    public Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    public String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    public MiniMessage miniMessage() {
        return MiniMessage.miniMessage();
    }

    public void sendMini(CommandSender target, String text) {
        sender(target).sendMessage(mini(text));
    }

    public void sendLegacy(CommandSender target, String text) {
        sender(target).sendMessage(legacy(text));
    }

    public void broadcastMini(String text) {
        all().sendMessage(mini(text));
    }
}
