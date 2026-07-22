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

/**
 * Shaded, relocated Adventure bridge shared by WonderEvents, its expansions and its addons.
 *
 * <p>Adventure (and {@code adventure-platform-bukkit}) is bundled inside the WonderEvents jar
 * and relocated to {@code voiidstudios.wonderevents.libs.adventure} at build time, so this
 * always works the same way regardless of whether the server is Spigot, Paper, or any Paper
 * fork, and regardless of whether that server ships Adventure natively or not:
 * <ul>
 *   <li>On Paper (which has native Adventure support), the underlying {@link BukkitAudiences}
 *       reaches the server's native implementation internally through reflection.</li>
 *   <li>On plain Spigot/Bukkit (which ships no Adventure at all), it falls back to sending
 *       legacy chat packets built from the same {@link Component}s.</li>
 * </ul>
 * Either way, expansions and addons only ever talk to this one API and never need to care
 * which platform they ended up running on.
 *
 * <p>Expansions/addons should not create this themselves; use
 * {@link voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext#getAdventure()} or
 * {@link voiidstudios.wonderevents.api.WonderBootstrap#getAdventure()} instead.
 */
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
                context.getPlugin().getYALogger().severe(
                        "No pude iniciar el puente de Adventure: " + exception.getMessage(), exception);
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

    /** Raw {@link BukkitAudiences} instance, for anything not covered by the helpers below. */
    public BukkitAudiences audiences() {
        if (audiences == null) {
            throw new IllegalStateException(
                    "El puente de Adventure de WonderEvents todavia no esta listo (plugin deshabilitado?)");
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

    /** Every currently connected player plus the console. */
    public Audience all() {
        return audiences().all();
    }

    /** Parses MiniMessage markup (e.g. {@code "<red>Hola <bold>mundo</bold>"}) into a Component. */
    public Component mini(String text) {
        return miniMessage().deserialize(text == null ? "" : text);
    }

    /** Parses legacy '&' color codes (e.g. {@code "&cHola &lmundo"}) into a Component. */
    public Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    /** Strips a Component down to plain text, with no colors or formatting. */
    public String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    public MiniMessage miniMessage() {
        return MiniMessage.miniMessage();
    }

    /** Shortcut: parses {@code text} as MiniMessage and sends it straight away. */
    public void sendMini(CommandSender target, String text) {
        sender(target).sendMessage(mini(text));
    }

    /** Shortcut: parses {@code text} as legacy '&' codes and sends it straight away. */
    public void sendLegacy(CommandSender target, String text) {
        sender(target).sendMessage(legacy(text));
    }

    /** Shortcut: parses {@code text} as MiniMessage and broadcasts it to every player + console. */
    public void broadcastMini(String text) {
        all().sendMessage(mini(text));
    }
}
