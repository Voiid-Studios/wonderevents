package voiidstudios.wonderevents.expansions;

import dev.faststats.Attributes;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import voiidstudios.wonderevents.api.WEABootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;
import voiidstudios.wonderevents.core.manifest.ServerVersionUtil;
import voiidstudios.wonderevents.core.manifest.PlatformDependencyChecker;
import voiidstudios.wonderevents.core.loader.FeatureClassLoader;
import voiidstudios.wonderevents.core.metrics.MetricsManager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WonderExpansionManager {
    private final PluginContext context;
    private final YALogger logger;
    private final File folder;
    private final Map<String, WonderExpansionEntry> loaded = new LinkedHashMap<>();
    private final java.util.Set<String> disabled = new java.util.LinkedHashSet<>();

    public WonderExpansionManager(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
        this.folder = new File(context.getPlugin().getDataFolder(), "expansions");
    }

    public int loadExpansions() {
        ensureFolder();

        File[] jars = folder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
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

                WonderExpansionEntry entry = loadEntry(jar, manifest);
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
                } catch (Throwable e) {
                    logger.passiveSevere("[Expansions] Failed to load expansion '" + expansionId + "': onLoad() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                    track(e, "onLoad", expansionId);
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                WonderManifest manifest = WonderManifestLoader.load(jar, logger);
                String missing = manifest == null ? "an unknown dependency" : describeMissingDependency(manifest);
                logger.passiveWarning("[Expansions] Could not load " + jar.getName() + " because a dependency is missing: " + missing);
            }
        }

        return loadedNow;
    }

    public void enableExpansions() {
        for (WonderExpansionEntry entry : loaded.values()) {
            try {
                entry.getExpansion().onEnable();
            } catch (Throwable e) {
                logger.passiveSevere("[Expansions] Failed to enable expansion '" + entry.getDescriptor().getName() + "': onEnable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                track(e, "onEnable", entry.getDescriptor().getName());
            }
        }
    }

    public int reloadExpansions() {
        ensureFolder();

        Map<String, File> currentJars = discoverExpansionJars();
        List<String> previousOrder = new ArrayList<>(loaded.keySet());

        disableMissingExpansions(currentJars);

        java.util.Set<String> newlyLoaded = loadNewExpansions(currentJars);
        reloadPresentExpansions(currentJars, previousOrder, newlyLoaded);

        logger.success("[Expansions] Reloaded! Active expansions: " + loaded.size());
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

            WonderExpansionEntry entry = loaded.remove(id);
            this.disabled.remove(id);
            if (entry == null) {
                continue;
            }

            String name = entry.getDescriptor().getName();
            try {
                entry.getExpansion().onDisable();
            } catch (Throwable e) {
                logger.passiveSevere("[Expansions] Failed to disable expansion '" + name + "': onDisable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                track(e, "onDisable", name);
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Throwable cleanupError) {
                logger.passiveSevere("[Expansions] Failed to clean up expansion '" + name + "': runtime cleanup threw an error. Please contact the expansion developer. Details: " + cleanupError.getMessage());
                track(cleanupError, "cleanup", name);
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

                WonderExpansionEntry entry = loadEntry(jar, manifest);
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
                    } catch (Throwable enableError) {
                        logger.passiveSevere("[Expansions] Failed to enable expansion '" + entry.getDescriptor().getName() + "': onEnable() threw an error. Please contact the expansion developer. Details: " + enableError.getMessage());
                        track(enableError, "onEnable", entry.getDescriptor().getName());
                    }

                    logger.success("[Expansions] Loaded expansion: " + entry.getDescriptor().getName());
                    progress = true;
                } catch (Throwable e) {
                    logger.passiveSevere("[Expansions] Failed to load expansion '" + expansionId + "': onLoad() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
                    track(e, "onLoad", expansionId);
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                WonderManifest manifest = WonderManifestLoader.load(jar, logger);
                String missing = manifest == null ? "an unknown dependency" : describeMissingDependency(manifest);
                logger.passiveWarning("[Expansions] Could not load " + jar.getName() + " because a dependency is missing: " + missing);
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

            WonderExpansionEntry entry = loaded.get(id);
            if (entry == null) {
                continue;
            }

            try {
                entry.getExpansion().onReload();
                reloaded++;
            } catch (Throwable e) {
                logger.passiveWarning("[Expansions] Failed to reload expansion '" + entry.getDescriptor().getName() + "': onReload() threw an error. Details: " + e.getMessage());
                track(e, "onReload", entry.getDescriptor().getName());
            }
        }

        return reloaded;
    }

    public void disableExpansions() {
        List<WonderExpansionEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (WonderExpansionEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getExpansion().onDisable();
            } catch (Throwable e) {
                logger.passiveWarning("[Expansions] Failed to disable expansion '" + name + "': " + e.getMessage());
                track(e, "onDisable", name);
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Throwable cleanupError) {
                logger.passiveWarning("[Expansions] Cleanup failed for expansion '" + name + "': " + cleanupError.getMessage());
                track(cleanupError, "cleanup", name);
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Expansions] Unloaded expansion: " + name);
        }

        loaded.clear();
        disabled.clear();
    }

    public int getLoadedCount() {
        return loaded.size();
    }

    public List<WonderExpansionDescriptor> getLoadedDescriptors() {
        List<WonderExpansionDescriptor> descriptors = new ArrayList<>();
        for (WonderExpansionEntry entry : loaded.values()) {
            descriptors.add(entry.getDescriptor());
        }
        return Collections.unmodifiableList(descriptors);
    }

    public boolean isLoaded(String id) {
        return id != null && loaded.containsKey(id.toLowerCase());
    }

    public boolean isEnabled(String id) {
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        return loaded.containsKey(normalized) && !disabled.contains(normalized);
    }

    public ExpansionToggleResult enableExpansion(String id) {
        if (id == null) {
            return ExpansionToggleResult.NOT_FOUND;
        }

        String normalized = id.toLowerCase();
        WonderExpansionEntry entry = loaded.get(normalized);
        if (entry == null) {
            return ExpansionToggleResult.NOT_FOUND;
        }

        if (!disabled.contains(normalized)) {
            return ExpansionToggleResult.ALREADY;
        }

        try {
            entry.getExpansion().onEnable();
        } catch (Throwable e) {
            logger.passiveSevere("[Expansions] Failed to enable expansion '" + entry.getDescriptor().getName() + "': onEnable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
            track(e, "onEnable", entry.getDescriptor().getName());
        }

        disabled.remove(normalized);
        logger.passiveInfo("[Expansions] Enabled expansion: " + entry.getDescriptor().getName());
        return ExpansionToggleResult.SUCCESS;
    }

    public ExpansionToggleResult disableExpansion(String id) {
        if (id == null) {
            return ExpansionToggleResult.NOT_FOUND;
        }

        String normalized = id.toLowerCase();
        WonderExpansionEntry entry = loaded.get(normalized);
        if (entry == null) {
            return ExpansionToggleResult.NOT_FOUND;
        }

        if (disabled.contains(normalized)) {
            return ExpansionToggleResult.ALREADY;
        }

        try {
            entry.getExpansion().onDisable();
        } catch (Throwable e) {
            logger.passiveSevere("[Expansions] Failed to disable expansion '" + entry.getDescriptor().getName() + "': onDisable() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
            track(e, "onDisable", entry.getDescriptor().getName());
        }

        disabled.add(normalized);
        logger.passiveInfo("[Expansions] Disabled expansion: " + entry.getDescriptor().getName());
        return ExpansionToggleResult.SUCCESS;
    }

    public enum ExpansionToggleResult {
        SUCCESS,
        ALREADY,
        NOT_FOUND
    }

    public File getExpansionDataFolder(String id) {
        File expansionFolder = new File(folder, id == null ? "unknown" : id);
        if (!expansionFolder.exists()) {
            expansionFolder.mkdirs();
        }
        return expansionFolder;
    }

    public File getExpansionDataFolder(WonderExpansionDescriptor descriptor) {
        return getExpansionDataFolder(descriptor == null ? null : descriptor.getId());
    }

    private WonderExpansionEntry loadEntry(File jarFile, WonderManifest manifest) {
        URLClassLoader classLoader;
        try {
            classLoader = new FeatureClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    context.getPlugin().getClass().getClassLoader()
            );
        } catch (Throwable e) {
            logger.passiveWarning("[Expansions] Could not create a loader for " + manifest.getName() + ": " + e.getMessage());
            track(e, "classloader-creation", manifest.getId());
            return null;
        }

        WEABootstrap expansion;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WEABootstrap.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Expansions] The bootstrap class in " + manifest.getName() + " does not extend WEABootstrap.");
                closeQuietly(classLoader);
                return null;
            }
            expansion = (WEABootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (LinkageError e) {
            logger.passiveSevere("[Expansions] Could not start " + manifest.getName() + ": it looks like it was built against a different/older WonderEvents API (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). Ask the expansion developer to recompile it against this WonderEvents version.");
            track(e, "bootstrap-instantiation", manifest.getId());
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.passiveWarning("[Expansions] Could not start " + manifest.getName() + ": the bootstrap class could not be created. Details: " + e.getMessage());
            track(e, "bootstrap-instantiation", manifest.getId());
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

        try {
            expansion.init(featureContext);
        } catch (LinkageError e) {
            logger.passiveSevere("[Expansions] Could not start " + manifest.getName() + ": it looks like it was built against a different/older WonderEvents API (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). Ask the expansion developer to recompile it against this WonderEvents version.");
            track(e, "init", manifest.getId());
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.passiveSevere("[Expansions] Failed to initialize " + manifest.getName() + ": init() threw an error. Please contact the expansion developer. Details: " + e.getMessage());
            track(e, "init", manifest.getId());
            closeQuietly(classLoader);
            return null;
        }

        return new WonderExpansionEntry(expansion, featureContext, classLoader, new WonderExpansionDescriptor(manifest), jarFile);
    }

    private LoadDecision canLoad(WonderManifest manifest) {
        String minCore = manifest.getMinCoreVersion();
        if (minCore != null && !minCore.isBlank()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtLeast(current, minCore)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires WonderEvents " + minCore + " or newer, but the current version is " + current);
                return LoadDecision.REJECTED;
            }
        }

        String maxCore = manifest.getMaxCoreVersion();
        if (maxCore != null && !maxCore.isBlank()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtMost(current, maxCore)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires WonderEvents " + maxCore + " or older, but the current version is " + current);
                return LoadDecision.REJECTED;
            }
        }

        String minMc = manifest.getMinMinecraftVersion();
        if (minMc != null && !minMc.isBlank()) {
            String currentMc = ServerVersionUtil.getMinecraftVersion();
            if (!VersionUtil.isAtLeast(currentMc, minMc)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires Minecraft " + minMc + " or newer, but the server is running " + currentMc);
                return LoadDecision.REJECTED;
            }
        }

        String maxMc = manifest.getMaxMinecraftVersion();
        if (maxMc != null && !maxMc.isBlank()) {
            String currentMc = ServerVersionUtil.getMinecraftVersion();
            if (!VersionUtil.isAtMost(currentMc, maxMc)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires Minecraft " + maxMc + " or older, but the server is running " + currentMc);
                return LoadDecision.REJECTED;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getPluginDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(name -> Bukkit.getPluginManager().getPlugin(name) != null);
            if (rule.isRequired() && !satisfied) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires " + rule.describe() + " (plugins), but the condition is not met.");
                return LoadDecision.REJECTED;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getPlatformDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(PlatformDependencyChecker::isPresent);
            if (rule.isRequired() && !satisfied) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requires " + rule.describe() + " (platform), but the condition is not met.");
                return LoadDecision.REJECTED;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getExpansionDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(this::isLoaded);
            if (rule.isRequired() && !satisfied) {
                return LoadDecision.RETRY_LATER;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getAddonDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(id -> context.getAddonManager() != null && context.getAddonManager().isLoaded(id));
            if (rule.isRequired() && !satisfied) {
                return LoadDecision.RETRY_LATER;
            }
        }

        return LoadDecision.READY;
    }

    private String describeMissingDependency(WonderManifest manifest) {
        for (WonderManifest.DependencyRule rule : manifest.getExpansionDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(this::isLoaded);
            if (rule.isRequired() && !satisfied) {
                return "expansion " + rule.describe();
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getAddonDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(id -> context.getAddonManager() != null && context.getAddonManager().isLoaded(id));
            if (rule.isRequired() && !satisfied) {
                return "addon " + rule.describe();
            }
        }

        return "an unknown dependency";
    }

    private void track(Throwable e, String stage, String expansionId) {
        MetricsManager.getErrorTracker().trackError(e)
                .attributes(Attributes.empty()
                        .put("component", "expansion-manager")
                        .put("stage", stage)
                        .put("expansion", expansionId))
                .handled(true);
    }

    private void ensureFolder() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private void ensureExpansionFolder(WonderExpansionDescriptor descriptor) {
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
        } catch (Throwable ignored) {
        }
    }

    private enum LoadDecision {
        READY,
        RETRY_LATER,
        REJECTED
    }
}