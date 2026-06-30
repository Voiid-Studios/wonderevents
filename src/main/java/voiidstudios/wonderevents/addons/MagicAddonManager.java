package voiidstudios.wonderevents.addons;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.YALogger;

/**
 * Manages the complete addon lifecycle for D3V-ZE.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Scan {@code plugins/D3V-ZE/addons/} for {@code .jar} files.</li>
 *   <li>Load and enable each addon via {@link MagicAddonLoader}.</li>
 *   <li>Propagate lifecycle events: onLoad, onEnable, onReload, onDisable.</li>
 *   <li>Survive individual addon failures without crashing the main plugin.</li>
 *   <li>Support safe reload: disable → close class-loaders → re-scan → load.</li>
 * </ul>
 */
public final class MagicAddonManager {

    private final PluginContext context;
    private final YALogger      logger;
    private final File          addonsFolder;

    /** Ordered map of addon name → runtime entry. */
    private final Map<String, MagicAddonEntry> loaded = new LinkedHashMap<>();

    public MagicAddonManager(PluginContext context) {
        this.context      = context;
        this.logger       = context.getPlugin().getYALogger();
        this.addonsFolder = new File(context.getPlugin().getDataFolder(), "addons");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public lifecycle API (called from D3VZEBootstrap / ReloadCommand)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scans the addons folder, loads every valid jar, and calls
     * {@link MagicAddon#onLoad} on each one.
     *
     * <p>Call this right after core configs are ready but before gameplay
     * modules finish initializing.
     *
     * @return number of addons successfully loaded
     */
    public int loadAddons() {
        ensureAddonsFolder();

        File[] jars = addonsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.passiveInfo("[Addons] No encontre addons en la carpeta addons/");
            return 0;
        }

        MagicAddonLoader loader = new MagicAddonLoader(context);
        int count = 0;

        for (File jar : jars) {
            if (loaded.containsKey(normalizeJarName(jar))) {
                logger.passiveWarning("[Addons] Addon ya cargado; lo omito: " + jar.getName());
                continue;
            }

            MagicAddonEntry entry = null;
            try {
                entry = loader.load(jar);
            } catch (Exception e) {
                logger.passiveWarning("[Addons] Error inesperado al cargar " + jar.getName()
                        + ": " + e.getMessage());
            }

            if (entry == null) {
                continue; // loader already logged a specific reason
            }

            String addonName = entry.getDescriptor().getName();

            // Guard against name collision between two jars
            if (loaded.containsKey(addonName)) {
                logger.passiveWarning("[Addons] Ya existe un addon con el nombre '" + addonName + "'. Omito " + jar.getName());
                entry.closeClassLoader();
                continue;
            }

            try {
                entry.getAddon().onLoad(entry.getContext());
                loaded.put(addonName, entry);
                logger.success("[Addons] Addon despierto: " + entry.getDescriptor());
                count++;
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onLoad() fallo en el addon '" + addonName + "': " + e.getMessage());
                entry.closeClassLoader();
            }
        }

        return count;
    }

    /**
     * Calls {@link MagicAddon#onEnable} on every successfully loaded addon.
     *
     * <p>Call this after all D3V-ZE gameplay modules have finished initializing.
     */
    public void enableAddons() {
        for (MagicAddonEntry entry : loaded.values()) {
            try {
                entry.getAddon().onEnable();
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onEnable() fallo en el addon '" + entry.getDescriptor().getName() + "': " + e.getMessage());
            }
        }
    }

    /**
     * Reloads all addons safely.
     *
     * <p>Order: disable all → close class-loaders → clear state → re-scan → load → enable.
     */
    public void reloadAddons() {
        disableAndUnloadAll();
        int count = loadAddons();
        enableAddons();
        logger.success("[Addons] Recarga completada. Addons activos: " + count);
    }

    /**
     * Disables all addons and closes their class-loaders.
     * Called on plugin shutdown or before reload.
     */
    public void disableAddons() {
        disableAndUnloadAll();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns an unmodifiable snapshot of loaded addon descriptors. */
    public List<MagicAddonDescriptor> getLoadedDescriptors() {
        List<MagicAddonDescriptor> list = new ArrayList<>();
        for (MagicAddonEntry e : loaded.values()) {
            list.add(e.getDescriptor());
        }
        return Collections.unmodifiableList(list);
    }

    /** Returns how many addons are currently loaded. */
    public int getLoadedCount() {
        return loaded.size();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal
    // ──────────────────────────────────────────────────────────────────────────

    private void disableAndUnloadAll() {
        // Reverse order for clean teardown
        List<MagicAddonEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (MagicAddonEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getAddon().onDisable();
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onDisable() fallo en el addon '" + name + "': " + e.getMessage());
            }

            // Clean up all runtime resources registered by this addon before the
            // class-loader is closed, so no stale listeners/commands/handlers survive.
            try {
                entry.getContext().cleanupRuntimeRegistrations();
            } catch (Exception cleanupError) {
                logger.passiveWarning("[Addons] La limpieza fallo en el addon '" + name + "': " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Addons] Addon dormido: " + name);
        }

        loaded.clear();
    }

    private void ensureAddonsFolder() {
        if (!addonsFolder.exists() && addonsFolder.mkdirs()) {}
    }

    /** Normalizes a jar file to a stable key for duplicate-detection before we know the addon name. */
    private String normalizeJarName(File jar) {
        return jar.getName().toLowerCase();
    }
}
