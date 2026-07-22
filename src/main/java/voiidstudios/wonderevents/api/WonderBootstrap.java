package voiidstudios.wonderevents.api;

import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.managers.AdventureManager;
import voiidstudios.wonderevents.core.manifest.WonderManifest;

/**
 * Base lifecycle for WonderEvents expansions and addons.
 *
 * <p>Implementations receive the same four lifecycle points:
 * <ul>
 *   <li>{@link #onLoad(WonderFeatureContext)}</li>
 *   <li>{@link #onEnable()}</li>
 *   <li>{@link #onReload()}</li>
 *   <li>{@link #onDisable()}</li>
 * </ul>
 *
 * <p>The runtime context is injected by the core before {@code onLoad()} is called.
 */
public abstract class WonderBootstrap {

    private WonderFeatureContext featureContext;
    private WonderManifest manifest;

    /** Internal wiring used by the core before lifecycle callbacks begin. */
    public final void init(WonderFeatureContext context) {
        this.featureContext = context;
        this.manifest = context == null ? null : context.getManifest();
    }

    /** Called once after the feature context is ready but before the feature is enabled. */
    public void onLoad(WonderFeatureContext context) {
        // default no-op
    }

    /** Called after every feature has been loaded and the core is ready for gameplay. */
    public void onEnable() {
        // default no-op
    }

    /** Called when WonderEvents is reloaded. */
    public void onReload() {
        // default no-op
    }

    /** Called when WonderEvents shuts down or a feature is unloaded. */
    public void onDisable() {
        // default no-op
    }

    public final WonderFeatureContext getFeatureContext() {
        return featureContext;
    }

    public final PluginContext getPluginContext() {
        return featureContext == null ? null : featureContext.getPluginContext();
    }

    public final WEBootstrap getCore() {
        return featureContext == null ? null : featureContext.getCore();
    }

    public final JavaPlugin getPlugin() {
        return featureContext == null ? null : featureContext.getPlugin();
    }

    public final WonderManifest getManifest() {
        return manifest;
    }

    public final YALogger getLogger() {
        return featureContext == null ? null : featureContext.getLogger();
    }

    /**
     * Simple entry point for Adventure: {@code getAdventure().player(p).sendMessage(getAdventure().mini("<red>Hola"))}.
     * Works the same way on Paper and Spigot, from 1.16 up.
     */
    public final AdventureManager getAdventure() {
        return featureContext == null ? null : featureContext.getAdventure();
    }
}
