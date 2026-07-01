package voiidstudios.wonderevents.addons;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class for all D3V-ZE addons.
 *
 * <p>Every addon jar must contain a class that extends this and is declared as
 * the {@code main} field in {@code addon.yml}.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #onLoad(MagicAddonContext)} — called once after core configs are
 *       ready. Register listeners, commands, and config defaults here.</li>
 *   <li>{@link #onEnable()} — called after the full D3V-ZE feature set is
 *       initialized. Safe to interact with all managers here.</li>
 *   <li>{@link #onReload()} — called during {@code /d3vze reload}. Reload
 *       your own configs and refresh any cached state.</li>
 *   <li>{@link #onDisable()} — called on plugin shutdown or before a reload
 *       unloads this addon. Clean up resources here.</li>
 * </ol>
 */
public abstract class MagicAddon {

    private MagicAddonContext addonContext;
    private MagicAddonDescriptor descriptor;

    // -------------------------------------------------------------------------
    // Framework-internal wiring (called by D3VAddonLoader, not by addons)
    // -------------------------------------------------------------------------

    /** @hidden Called by the loader before onLoad. */
    final void init(MagicAddonContext addonContext, MagicAddonDescriptor descriptor) {
        this.addonContext = addonContext;
        this.descriptor   = descriptor;
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks — override as needed
    // -------------------------------------------------------------------------

    /**
     * Called right after core configs are ready, before gameplay modules have
     * finished initializing.
     *
     * <p>Use this to register listeners, commands, and config defaults.
     *
     * @param context the addon context, providing access to plugin systems
     */
    public void onLoad(MagicAddonContext context) {}

    /**
     * Called after D3V-ZE has finished loading all gameplay modules and events.
     * Use this for anything that requires fully initialized plugin systems.
     */
    public void onEnable() {}

    /**
     * Called during {@code /d3vze reload}. Reload your YAML configs and
     * refresh any state that depends on them.
     */
    public void onReload() {}

    /**
     * Called on plugin shutdown (or just before reload unloads the addon).
     * Unregister resources and clean up state here.
     */
    public void onDisable() {}

    // -------------------------------------------------------------------------
    // Accessors available to subclasses
    // -------------------------------------------------------------------------

    /** Returns the context object that gives this addon access to plugin systems. */
    protected final MagicAddonContext getAddonContext() {
        return addonContext;
    }

    /** Returns this addon's parsed metadata from {@code addon.yml}. */
    protected final MagicAddonDescriptor getDescriptor() {
        return descriptor;
    }

    /** Convenience alias for {@link MagicAddonContext#getPluginContext()}. */
    protected final voiidstudios.wonderevents.core.PluginContext getPluginContext() {
        return addonContext.getPluginContext();
    }

    /** Convenience alias for the core plugin instance. */
    protected final JavaPlugin getPlugin() {
        return addonContext.getPlugin();
    }

    /** Convenience alias for the core plugin instance. */
    protected final JavaPlugin getCore() {
        return addonContext.getCore();
    }

}
