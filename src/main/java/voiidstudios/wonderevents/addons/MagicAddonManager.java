package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/**
 * Manages the addon lifecycle for WonderEvents.
 */
public final class MagicAddonManager {

    private final PluginContext context;
    private final YALogger logger;
    private final File addonsFolder;

    private final Map<String, MagicAddonEntry> loaded = new LinkedHashMap<>();

    public MagicAddonManager(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
        this.addonsFolder = new File(context.getPlugin().getDataFolder(), "addons");
    }

    public int loadAddons() {
        ensureAddonsFolder();

        File[] jars = addonsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.passiveInfo("[Addons] No encontre addons en addons/");
            return 0;
        }

        List<File> pending = new ArrayList<>();
        Collections.addAll(pending, jars);

        MagicAddonLoader loader = new MagicAddonLoader(context);
        java.util.Set<String> loadedIds = new java.util.LinkedHashSet<>();
        boolean progress;

        do {
            progress = false;
            List<File> nextPending = new ArrayList<>();

            for (File jar : pending) {
                WonderManifest manifest = WonderManifestLoader.load(jar, logger);
                if (manifest == null) {
                    continue;
                }

                if (manifest.getBootstrap() == null || manifest.getBootstrap().isBlank()) {
                    logger.passiveWarning("[Addons] " + jar.getName() + " no define bootstrap.");
                    continue;
                }

                LoadDecision decision = canLoad(manifest);
                if (decision == LoadDecision.RETRY_LATER) {
                    nextPending.add(jar);
                    continue;
                }
                if (decision == LoadDecision.REJECTED) {
                    continue;
                }

                String addonId = manifest.getId().toLowerCase();
                if (loaded.containsKey(addonId)) {
                    logger.passiveWarning("[Addons] Ya existe un addon con el id '" + addonId + "'. Omito " + jar.getName());
                    continue;
                }

                MagicAddonEntry entry = loader.load(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getAddon().onLoad(entry.getContext());
                    loaded.put(addonId, entry);
                    logger.success("[Addons] Addon despierto: " + entry.getDescriptor());
                    loadedIds.add(addonId);
                    progress = true;
                } catch (Exception e) {
                    logger.passiveWarning("[Addons] onLoad() fallo en el addon '" + addonId + "': " + e.getMessage());
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                logger.passiveWarning("[Addons] Sigo sin poder cargar " + jar.getName() + " porque le faltan dependencias.");
            }
        }

        return loadedIds.size();
    }

    public void enableAddons() {
        for (MagicAddonEntry entry : loaded.values()) {
            try {
                entry.getAddon().onEnable();
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onEnable() fallo en el addon '" + entry.getDescriptor().getName() + "': " + e.getMessage());
            }
        }
    }

    public int reloadAddons() {
        ensureAddonsFolder();

        Map<String, File> currentJars = discoverAddonJars();
        List<String> previousOrder = new ArrayList<>(loaded.keySet());

        int disabled = disableMissingAddons(currentJars);
        java.util.Set<String> newlyLoaded = loadNewAddons(currentJars);
        int reloaded = reloadPresentAddons(currentJars, previousOrder, newlyLoaded);

        logger.success("[Addons] Recarga completada. Addons activos: " + loaded.size()
                + " (nuevos: " + newlyLoaded.size() + ", eliminados: " + disabled + ", recargados: " + reloaded + ")");
        return loaded.size();
    }

    private Map<String, File> discoverAddonJars() {
        Map<String, File> jars = new LinkedHashMap<>();
        File[] files = addonsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (files == null) {
            return jars;
        }

        for (File file : files) {
            WonderManifest manifest = WonderManifestLoader.load(file, logger);
            if (manifest == null) {
                continue;
            }

            String id = manifest.getId().toLowerCase();
            if (jars.containsKey(id)) {
                logger.passiveWarning("[Addons] Ya existe un addon con el id '" + id + "'. Omito " + file.getName());
                continue;
            }

            jars.put(id, file);
        }
        return jars;
    }

    private int disableMissingAddons(Map<String, File> currentJars) {
        int disabled = 0;
        for (String id : new ArrayList<>(loaded.keySet())) {
            if (currentJars.containsKey(id)) {
                continue;
            }

            MagicAddonEntry entry = loaded.remove(id);
            if (entry == null) {
                continue;
            }

            String name = entry.getDescriptor().getName();
            try {
                entry.getAddon().onDisable();
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onDisable() fallo en el addon '" + name + "': " + e.getMessage());
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Exception cleanupError) {
                logger.passiveWarning("[Addons] La limpieza fallo en el addon '" + name + "': " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Addons] Addon dormido: " + name);
            disabled++;
        }
        return disabled;
    }

    private java.util.Set<String> loadNewAddons(Map<String, File> currentJars) {
        List<File> pending = new ArrayList<>();
        for (String id : currentJars.keySet()) {
            if (loaded.containsKey(id)) {
                continue;
            }
            pending.add(currentJars.get(id));
        }

        MagicAddonLoader loader = new MagicAddonLoader(context);
        java.util.Set<String> loadedIds = new java.util.LinkedHashSet<>();
        boolean progress;

        do {
            progress = false;
            List<File> nextPending = new ArrayList<>();

            for (File jar : pending) {
                WonderManifest manifest = WonderManifestLoader.load(jar, logger);
                if (manifest == null) {
                    continue;
                }

                if (manifest.getBootstrap() == null || manifest.getBootstrap().isBlank()) {
                    logger.passiveWarning("[Addons] " + jar.getName() + " no define bootstrap.");
                    continue;
                }

                LoadDecision decision = canLoad(manifest);
                if (decision == LoadDecision.RETRY_LATER) {
                    nextPending.add(jar);
                    continue;
                }
                if (decision == LoadDecision.REJECTED) {
                    continue;
                }

                String addonId = manifest.getId().toLowerCase();
                if (loaded.containsKey(addonId)) {
                    continue;
                }

                MagicAddonEntry entry = loader.load(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getAddon().onLoad(entry.getContext());
                    loaded.put(addonId, entry);
                    loadedIds.add(addonId);
                    try {
                        entry.getAddon().onEnable();
                    } catch (Exception enableError) {
                        logger.passiveWarning("[Addons] onEnable() fallo en el addon '" + entry.getDescriptor().getName() + "': " + enableError.getMessage());
                    }
                    logger.success("[Addons] Addon despierto: " + entry.getDescriptor());
                    progress = true;
                } catch (Exception e) {
                    logger.passiveWarning("[Addons] onLoad() fallo en el addon '" + addonId + "': " + e.getMessage());
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                logger.passiveWarning("[Addons] Sigo sin poder cargar " + jar.getName() + " porque le faltan dependencias.");
            }
        }

        return loadedIds;
    }

    private int reloadPresentAddons(Map<String, File> currentJars, List<String> previousOrder, java.util.Set<String> newlyLoaded) {
        int reloaded = 0;
        for (String id : previousOrder) {
            if (!currentJars.containsKey(id) || newlyLoaded.contains(id)) {
                continue;
            }

            MagicAddonEntry entry = loaded.get(id);
            if (entry == null) {
                continue;
            }

            try {
                entry.getAddon().onReload();
                reloaded++;
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onReload() fallo en el addon '" + entry.getDescriptor().getName() + "': " + e.getMessage());
            }
        }
        return reloaded;
    }

    public void disableAddons() {
        List<MagicAddonEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (MagicAddonEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getAddon().onDisable();
            } catch (Exception e) {
                logger.passiveWarning("[Addons] onDisable() fallo en el addon '" + name + "': " + e.getMessage());
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Exception cleanupError) {
                logger.passiveWarning("[Addons] La limpieza fallo en el addon '" + name + "': " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Addons] Addon dormido: " + name);
        }

        loaded.clear();
    }

    public List<MagicAddonDescriptor> getLoadedDescriptors() {
        List<MagicAddonDescriptor> list = new ArrayList<>();
        for (MagicAddonEntry entry : loaded.values()) {
            list.add(entry.getDescriptor());
        }
        return Collections.unmodifiableList(list);
    }

    public int getLoadedCount() {
        return loaded.size();
    }

    public boolean isLoaded(String id) {
        return id != null && loaded.containsKey(id.toLowerCase());
    }

    private LoadDecision canLoad(WonderManifest manifest) {
        String required = manifest.getMinCoreVersion();
        if (required != null && !required.isBlank()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtLeast(current, required)) {
                logger.passiveWarning("[Addons] " + manifest.getName() + " requiere WonderEvents >= " + required + " y tienes " + current);
                return LoadDecision.REJECTED;
            }
        }

        for (var entry : manifest.getPluginDependencies().entrySet()) {
            boolean present = context.getPlugin().getServer().getPluginManager().getPlugin(entry.getKey()) != null;
            if (entry.getValue().isRequired() && !present) {
                logger.passiveWarning("[Addons] " + manifest.getName() + " requiere el plugin " + entry.getKey() + " y no esta presente.");
                return LoadDecision.REJECTED;
            }
        }

        for (var entry : manifest.getExpansionDependencies().entrySet()) {
            boolean present = context.getExpansionManager() != null && context.getExpansionManager().isLoaded(entry.getKey());
            if (entry.getValue().isRequired() && !present) {
                return LoadDecision.RETRY_LATER;
            }
        }

        for (var entry : manifest.getAddonDependencies().entrySet()) {
            boolean present = isLoaded(entry.getKey());
            if (entry.getValue().isRequired() && !present) {
                return LoadDecision.RETRY_LATER;
            }
        }

        return LoadDecision.READY;
    }

    private void registerManifestPermissions(MagicAddonContext featureContext, WonderManifest manifest) {
        if (featureContext == null || manifest == null || manifest.getPermissions().isEmpty()) {
            return;
        }

        for (WonderManifest.PermissionDefinition definition : manifest.getPermissions().values()) {
            Permission permission = new Permission(
                    definition.getName(),
                    definition.getDescription(),
                    parsePermissionDefault(definition.getDefaultValue()),
                    new LinkedHashMap<>(definition.getChildren())
            );
            featureContext.registerPermission(permission);
        }
    }

    private PermissionDefault parsePermissionDefault(String value) {
        if (value == null || value.isBlank()) {
            return PermissionDefault.OP;
        }

        switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true":
            case "yes":
            case "on":
                return PermissionDefault.TRUE;
            case "false":
            case "no":
            case "off":
                return PermissionDefault.FALSE;
            case "notop":
            case "not_op":
            case "not-op":
                return PermissionDefault.NOT_OP;
            case "op":
            default:
                return PermissionDefault.OP;
        }
    }

    private void ensureAddonsFolder() {
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }
    }

    private enum LoadDecision {
        READY,
        RETRY_LATER,
        REJECTED
    }
}
