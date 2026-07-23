package voiidstudios.wonderevents.api;

import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.managers.AdventureManager;
import voiidstudios.wonderevents.core.manifest.WonderManifest;

public abstract class WEABootstrap {
    private WonderFeatureContext featureContext;
    private WonderManifest manifest;

    public final void init(WonderFeatureContext context) {
        this.featureContext = context;
        this.manifest = context == null ? null : context.getManifest();
    }

    public void onLoad(WonderFeatureContext context) {}
    public void onEnable() {}
    public void onReload() {}
    public void onDisable() {}

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

    public final YALogger getLogger() {
        return featureContext == null ? null : featureContext.getLogger();
    }

    public final AdventureManager getAdventure() {
        return featureContext == null ? null : featureContext.getAdventure();
    }

    public final WonderManifest getManifest() {
        return manifest;
    }
}
