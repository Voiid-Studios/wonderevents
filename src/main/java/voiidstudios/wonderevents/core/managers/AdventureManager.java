package voiidstudios.wonderevents.core.managers;

import voiidstudios.tsunamilib.TsunamiLib;
import voiidstudios.tsunamilib.libs.adventure.audience.Audience;
import voiidstudios.tsunamilib.libs.adventure.platform.bukkit.BukkitAudiences;
import voiidstudios.tsunamilib.libs.adventure.text.Component;
import voiidstudios.tsunamilib.libs.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import voiidstudios.wonderevents.core.PluginContext;

import java.util.UUID;

/** WonderEvents facade over the Adventure bridge owned by TsunamiLib. */
public final class AdventureManager {
    private voiidstudios.tsunamilib.managers.AdventureManager delegate;

    public AdventureManager(PluginContext context) {
    }

    public void start() {
        if (delegate != null) {
            return;
        }

        voiidstudios.tsunamilib.managers.AdventureManager shared = TsunamiLib.getAPI().getAdventureManager();
        if (!shared.isReady()) {
            throw new IllegalStateException("TsunamiLib's Adventure bridge is not ready");
        }
        delegate = shared;
    }

    public void stop() {
        // TsunamiLib owns the shared bridge; only detach this facade.
        delegate = null;
    }

    public boolean isReady() {
        return delegate != null && delegate.isReady();
    }

    public BukkitAudiences audiences() {
        if (!isReady()) {
            throw new IllegalStateException("The WonderEvents Adventure bridge is not ready yet (plugin disabled?)");
        }
        return delegate.audiences();
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
        audiences();
        return delegate.mini(text);
    }

    public Component legacy(String text) {
        audiences();
        return delegate.legacy(text);
    }

    public String plain(Component component) {
        audiences();
        return delegate.plain(component);
    }

    public MiniMessage miniMessage() {
        audiences();
        return delegate.miniMessage();
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
