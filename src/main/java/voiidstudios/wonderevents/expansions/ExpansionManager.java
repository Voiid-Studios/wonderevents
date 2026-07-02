package voiidstudios.wonderevents.expansions;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import voiidstudios.wonderevents.api.WonderBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;
import voiidstudios.wonderevents.core.loader.FeatureClassLoader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles WonderEvents expansions.
 */
public final class ExpansionManager {

    private final PluginContext context;
    private final YALogger logger;
    private final File folder;
    private final Map<String, ExpansionEntry> loaded = new LinkedHashMap<>();

    public ExpansionManager(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
        this.folder = new File(context.getPlugin().getDataFolder(), "expansions");
    }

    public int loadExpansions() {
        ensureFolder();

        File[] jars = folder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.passiveInfo("[Expansions] No expansions found in expansions/");
            return 0;
        }

        List<File> pending = new ArrayList<>();
        Collections.addAll(pending, jars);

        int loadedNow = 0;
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
                    logger.passiveSevere("[Expansions] Failed to load " + jar.getName() + ": the bootstrap class is missing in wonder-manifest.yml. Please contact the expansion developer.");
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

                String expansionId = manifest.getId().toLowerCase();
                if (loaded.containsKey(expansionId)) {
                    logger.passiveWarning("[Expansions] An expansion with the id '" + expansionId + "' is already loaded. Skipping " + jar.getName());
                    continue;
                }

                ExpansionEntry entry = loadEntry(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getExpansion().onLoad(entry.getContext());
                    loaded.put(expansionId, entry);
                    ensureExpansionFolder(entry.getDescriptor());

                    logger.success("[Expansions] Loaded expansion: " + entry.getDescriptor().getName());
                    loadedNow++;
                    progress = true;
                } catch (Exception e) {
                    logger.passiveSevere("[Expansions] Failed to load expansion '" + expansionId + "': onLoad() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                logger.passiveWarning("[Expansions] Could not load " + jar.getName() + " because some required dependencies are still missing.");
            }
        }

        return loadedNow;
    }

    public void enableExpansions() {
        for (ExpansionEntry entry : loaded.values()) {
            try {
                entry.getExpansion().onEnable();
            } catch (Exception e) {
                logger.passiveSevere("[Expansions] Failed to enable expansion '" + entry.getDescriptor().getName() + "': onEnable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
            }
        }
    }

    public int reloadExpansions() {
        ensureFolder();

        Map<String, File> currentJars = discoverExpansionJars();
        List<String> previousOrder = new ArrayList<>(loaded.keySet());

        int disabled = disableMissingExpansions(currentJars);
        java.util.Set<String> newlyLoaded = loadNewExpansions(currentJars);
        int reloaded = reloadPresentExpansions(currentJars, previousOrder, newlyLoaded);

        logger.success("[Expansions] Reload complete. Active expansions: " + loaded.size());
        return loaded.size();
    }

    private Map<String, File> discoverExpansionJars() {
        Map<String, File> jars = new LinkedHashMap<>();
        File[] files = folder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
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
                logger.passiveWarning("[Expansions] An expansion with the id '" + id + "' is already present. Skipping " + file.getName());
                continue;
            }

            jars.put(id, file);
        }
        return jars;
    }

    private int disableMissingExpansions(Map<String, File> currentJars) {
        int disabled = 0;

        for (String id : new ArrayList<>(loaded.keySet())) {
            if (currentJars.containsKey(id)) {
                continue;
            }

            ExpansionEntry entry = loaded.remove(id);
            if (entry == null) {
                continue;
            }

            String name = entry.getDescriptor().getName();
            try {
                entry.getExpansion().onDisable();
            } catch (Exception e) {
                logger.passiveSevere("[Expansions] Failed to disable expansion '" + name + "': onDisable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Exception cleanupError) {
                logger.passiveSevere("[Expansions] Failed to clean up expansion '" + name + "': runtime cleanup threw an error. Please contact the expansion developer. Details: " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Expansions] Unloaded expansion: " + name);
            disabled++;
        }

        return disabled;
    }

    private java.util.Set<String> loadNewExpansions(Map<String, File> currentJars) {
        List<File> pending = new ArrayList<>();
        for (String id : currentJars.keySet()) {
            if (loaded.containsKey(id)) {
                continue;
            }
            pending.add(currentJars.get(id));
        }

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
                    logger.passiveSevere("[Expansions] Failed to load " + jar.getName() + ": the bootstrap class is missing in wonder-manifest.yml. Please contact the expansion developer.");
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

                String expansionId = manifest.getId().toLowerCase();
                if (loaded.containsKey(expansionId)) {
                    continue;
                }

                ExpansionEntry entry = loadEntry(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getExpansion().onLoad(entry.getContext());
                    loaded.put(expansionId, entry);
                    loadedIds.add(expansionId);
                    ensureExpansionFolder(entry.getDescriptor());

                    try {
                        entry.getExpansion().onEnable();
                    } catch (Exception enableError) {
                        logger.passiveSevere("[Expansions] Failed to enable expansion '" + entry.getDescriptor().getName() + "': onEnable() threw an error. Please contact the expansion developer. Details: " + enableError.getMessage());
                    }

                    logger.success("[Expansions] Loaded expansion: " + entry.getDescriptor().getName());
                    progress = true;
                } catch (Exception e) {
                    logger.passiveSevere("[Expansions] Failed to load expansion '" + expansionId + "': onLoad() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                logger.passiveWarning("[Expansions] Could not load " + jar.getName() + " because some required dependencies are still missing.");
            }
        }

        return loadedIds;
    }

    private int reloadPresentExpansions(Map<String, File> currentJars, List<String> previousOrder, java.util.Set<String> newlyLoaded) {
        int reloaded = 0;

        for (String id : previousOrder) {
            if (!currentJars.containsKey(id) || newlyLoaded.contains(id)) {
                continue;
            }

            ExpansionEntry entry = loaded.get(id);
            if (entry == null) {
                continue;
            }

            try {
                entry.getExpansion().onReload();
                reloaded++;
            } catch (Exception e) {
                logger.passiveWarning("[Expansions] Failed to reload expansion '" + entry.getDescriptor().getName() + "': onReload() threw an error. Details: " + e.getMessage());
            }
        }

        return reloaded;
    }

    public void disableExpansions() {
        List<ExpansionEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (ExpansionEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getExpansion().onDisable();
            } catch (Exception e) {
                logger.passiveWarning("[Expansions] Failed to disable expansion '" + name + "': " + e.getMessage());
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Exception cleanupError) {
                logger.passiveWarning("[Expansions] Cleanup failed for expansion '" + name + "': " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Expansions] Unloaded expansion: " + name);
        }

        loaded.clear();
    }

    public int getLoadedCount() {
        return loaded.size();
    }

    public List<ExpansionDescriptor> getLoadedDescriptors() {
        List<ExpansionDescriptor> descriptors = new ArrayList<>();
        for (ExpansionEntry entry : loaded.values()) {
            descriptors.add(entry.getDescriptor());
        }
        return Collections.unmodifiableList(descriptors);
    }

    public boolean isLoaded(String id) {
        return id != null && loaded.containsKey(id.toLowerCase());
    }

    public File getExpansionDataFolder(String id) {
        File expansionFolder = new File(folder, id == null ? "unknown" : id);
        if (!expansionFolder.exists()) {
            expansionFolder.mkdirs();
        }
        return expansionFolder;
    }

    public File getExpansionDataFolder(ExpansionDescriptor descriptor) {
        return getExpansionDataFolder(descriptor == null ? null : descriptor.getId());
    }

    private ExpansionEntry loadEntry(File jarFile, WonderManifest manifest) {
        URLClassLoader classLoader;
        try {
            classLoader = new FeatureClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    context.getPlugin().getClass().getClassLoader()
            );
        } catch (Exception e) {
            logger.passiveWarning("[Expansions] Could not create a loader for " + manifest.getName() + ": " + e.getMessage());
            return null;
        }

        WonderBootstrap expansion;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WonderBootstrap.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Expansions] The bootstrap class in " + manifest.getName() + " does not extend WonderBootstrap.");
                closeQuietly(classLoader);
                return null;
            }
            expansion = (WonderBootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.passiveWarning("[Expansions] Could not start " + manifest.getName() + ": the bootstrap class could not be created. Details: " + e.getMessage());
            closeQuietly(classLoader);
            return null;
        }

        WonderFeatureContext featureContext = new WonderFeatureContext(
                context,
                manifest,
                classLoader,
                getExpansionDataFolder(manifest.getId())
        );

        registerManifestPermissions(featureContext, manifest);
        expansion.init(featureContext);
        return new ExpansionEntry(expansion, featureContext, classLoader, new ExpansionDescriptor(manifest), jarFile);
    }

    private LoadDecision canLoad(WonderManifest manifest) {
        String required = manifest.getMinCoreVersion();
        if (required != null && !required.isBlank()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtLeast(current, required)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires WonderEvents " + required + " or newer, but the current version is " + current + ".");
                return LoadDecision.REJECTED;
            }
        }

        for (var entry : manifest.getPluginDependencies().entrySet()) {
            boolean present = Bukkit.getPluginManager().getPlugin(entry.getKey()) != null;
            if (entry.getValue().isRequired() && !present) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires the plugin '" + entry.getKey() + "', but it is not installed.");
                return LoadDecision.REJECTED;
            }
        }

        for (var entry : manifest.getExpansionDependencies().entrySet()) {
            boolean present = isLoaded(entry.getKey());
            if (entry.getValue().isRequired() && !present) {
                return LoadDecision.RETRY_LATER;
            }
        }

        for (var entry : manifest.getAddonDependencies().entrySet()) {
            boolean present = context.getAddonManager() != null && context.getAddonManager().isLoaded(entry.getKey());
            if (entry.getValue().isRequired() && !present) {
                return LoadDecision.RETRY_LATER;
            }
        }

        return LoadDecision.READY;
    }

    private void ensureFolder() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private void ensureExpansionFolder(ExpansionDescriptor descriptor) {
        getExpansionDataFolder(descriptor);
    }

    private void registerManifestPermissions(WonderFeatureContext featureContext, WonderManifest manifest) {
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

    private void closeQuietly(URLClassLoader cl) {
        try {
            cl.close();
        } catch (Exception ignored) {
        }
    }

    private enum LoadDecision {
        READY,
        RETRY_LATER,
        REJECTED
    }
}