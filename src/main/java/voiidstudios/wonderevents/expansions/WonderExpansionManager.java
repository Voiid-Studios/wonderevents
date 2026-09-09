package voiidstudios.wonderevents.expansions;

import dev.faststats.Attributes;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import voiidstudios.wonderevents.api.WEABootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.log.WonderLogMessages;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;
import voiidstudios.wonderevents.core.manifest.ServerVersionUtil;
import voiidstudios.wonderevents.core.manifest.PlatformDependencyChecker;
import voiidstudios.wonderevents.core.loader.FeatureClassLoader;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.utils.BundledContentExtractor;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WonderExpansionManager {
    private static final String PREFIX = "[Expansions] ";
    private static final String SUFFIX = "expansion";

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

                if (manifest.getBootstrap() == null || manifest.getBootstrap().trim().isEmpty()) {
                    logger.severe(PREFIX + WonderLogMessages.MISSING_BOOTSTRAP.format(jar.getName(), SUFFIX));
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
                    logger.warning(PREFIX + WonderLogMessages.ALREADY_LOADED.format(expansionId, loaded.get(expansionId).getDescriptor().getVersion(), SUFFIX, jar.getName()));
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

                    logger.success(PREFIX + "Loaded expansion: " + entry.getDescriptor().getName());
                    loadedNow++;
                    progress = true;
                } catch (Throwable e) {
                    logger.severe(PREFIX + WonderLogMessages.ONLOAD_ERROR.format(jar.getName(), SUFFIX));
                    logger.severe(e.getMessage());
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
                logger.severe(PREFIX + WonderLogMessages.MISSING_DEPENDENCY.format(jar.getName(), SUFFIX, missing));
            }
        }

        return loadedNow;
    }

    public void enableExpansions() {
        for (WonderExpansionEntry entry : loaded.values()) {
            try {
                entry.getExpansion().onEnable();
            } catch (Throwable e) {
                logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                logger.severe(e.getMessage());
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

        logger.success(PREFIX + "Reloaded! Active expansions: " + loaded.size());
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
                WonderManifest existingManifest = WonderManifestLoader.load(jars.get(id), logger);
                String existingVersion = existingManifest != null ? existingManifest.getVersion() : "?";
                logger.warning(PREFIX + WonderLogMessages.ALREADY_LOADED.format(id, existingVersion, SUFFIX, file.getName()));
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
                logger.severe(PREFIX + WonderLogMessages.ONDISABLE_ERROR.format(name, SUFFIX));
                logger.severe(e.getMessage());
                track(e, "onDisable", name);
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Throwable cleanupError) {
                logger.severe(PREFIX + WonderLogMessages.CLEANRUNTIME_ERROR.format(name, SUFFIX));
                logger.severe(cleanupError.getMessage());
                track(cleanupError, "cleanup", name);
            }

            entry.closeClassLoader();
            logger.passiveInfo(PREFIX + "Unloaded expansion: " + name);
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

                if (manifest.getBootstrap() == null || manifest.getBootstrap().trim().isEmpty()) {
                    logger.severe(PREFIX + WonderLogMessages.MISSING_BOOTSTRAP.format(jar.getName(), SUFFIX));
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
                        logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                        logger.severe(enableError.getMessage());
                        track(enableError, "onEnable", entry.getDescriptor().getName());
                    }

                    logger.success(PREFIX + "Loaded expansion: " + entry.getDescriptor().getName());
                    progress = true;
                } catch (Throwable e) {
                    logger.severe(PREFIX + WonderLogMessages.ONLOAD_ERROR.format(expansionId, SUFFIX));
                    logger.severe(e.getMessage());
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
                logger.severe(PREFIX + WonderLogMessages.MISSING_DEPENDENCY.format(jar.getName(), SUFFIX, missing));
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
                logger.severe(PREFIX + WonderLogMessages.ONRELOAD_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                logger.severe(e.getMessage());
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
                logger.severe(PREFIX + WonderLogMessages.ONDISABLE_ERROR.format(name, SUFFIX));
                logger.severe(e.getMessage());
                track(e, "onDisable", name);
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Throwable cleanupError) {
                logger.severe(PREFIX + WonderLogMessages.CLEANRUNTIME_ERROR.format(name, SUFFIX));
                logger.severe(cleanupError.getMessage());
                track(cleanupError, "cleanup", name);
            }

            entry.closeClassLoader();
            logger.passiveInfo(PREFIX + "Unloaded expansion: " + name);
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
            logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "onEnable", entry.getDescriptor().getName());
        }

        disabled.remove(normalized);
        logger.passiveInfo(PREFIX + "Enabled expansion: " + entry.getDescriptor().getName());
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
            logger.severe(PREFIX + WonderLogMessages.ONDISABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "onDisable", entry.getDescriptor().getName());
        }

        disabled.add(normalized);
        logger.passiveInfo(PREFIX + "Disabled expansion: " + entry.getDescriptor().getName());
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
            logger.severe(PREFIX + WonderLogMessages.INTERNAL_CLASSLOADER_ERROR.format(manifest.getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "classloader-creation", manifest.getId());
            return null;
        }

        WEABootstrap expansion;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WEABootstrap.class.isAssignableFrom(mainClass)) {
                logger.severe(PREFIX + WonderLogMessages.BOOTSTRAP_NOT_ASSIGNABLE.format(manifest.getName(), SUFFIX));
                closeQuietly(classLoader);
                return null;
            }
            expansion = (WEABootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (LinkageError e) {
            logger.severe(PREFIX + WonderLogMessages.API_VERSION_MISMATCH.format(manifest.getName(), SUFFIX, e.getClass().getSimpleName(), e.getMessage()));
            track(e, "bootstrap-instantiation", manifest.getId());
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.severe(PREFIX + WonderLogMessages.BOOTSTRAP_INSTANTIATION_ERROR.format(manifest.getName(), SUFFIX));
            logger.severe(e.getMessage());
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
            logger.severe(PREFIX + WonderLogMessages.API_VERSION_MISMATCH.format(manifest.getName(), SUFFIX, e.getClass().getSimpleName(), e.getMessage()));
            track(e, "init", manifest.getId());
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.severe(PREFIX + WonderLogMessages.INIT_ERROR.format(manifest.getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "init", manifest.getId());
            closeQuietly(classLoader);
            return null;
        }

        return new WonderExpansionEntry(expansion, featureContext, classLoader, new WonderExpansionDescriptor(manifest), jarFile);
    }

    private LoadDecision canLoad(WonderManifest manifest) {
        String minCore = manifest.getMinCoreVersion();
        if (minCore != null && !minCore.trim().isEmpty()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtLeast(current, minCore)) {
                logger.severe(PREFIX + WonderLogMessages.MIN_CORE_VERSION.format(manifest.getName(), SUFFIX, minCore, current));
                return LoadDecision.REJECTED;
            }
        }

        String maxCore = manifest.getMaxCoreVersion();
        if (maxCore != null && !maxCore.trim().isEmpty()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtMost(current, maxCore)) {
                logger.severe(PREFIX + WonderLogMessages.MAX_CORE_VERSION.format(manifest.getName(), SUFFIX, maxCore, current));
                return LoadDecision.REJECTED;
            }
        }

        String minMc = manifest.getMinMinecraftVersion();
        if (minMc != null && !minMc.trim().isEmpty()) {
            String currentMc = ServerVersionUtil.getMinecraftVersion();
            if (!VersionUtil.isAtLeast(currentMc, minMc)) {
                logger.severe(PREFIX + WonderLogMessages.MIN_MINECRAFT_VERSION.format(manifest.getName(), SUFFIX, minMc, currentMc));
                return LoadDecision.REJECTED;
            }
        }

        String maxMc = manifest.getMaxMinecraftVersion();
        if (maxMc != null && !maxMc.trim().isEmpty()) {
            String currentMc = ServerVersionUtil.getMinecraftVersion();
            if (!VersionUtil.isAtMost(currentMc, maxMc)) {
                logger.severe(PREFIX + WonderLogMessages.MAX_MINECRAFT_VERSION.format(manifest.getName(), SUFFIX, maxMc, currentMc));
                return LoadDecision.REJECTED;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getPluginDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(name -> Bukkit.getPluginManager().getPlugin(name) != null);
            if (rule.isRequired() && !satisfied) {
                logger.severe(PREFIX + WonderLogMessages.MISSING_PLUGIN_DEPENDENCY.format(manifest.getName(), SUFFIX, rule.describe()));
                return LoadDecision.REJECTED;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getPlatformDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(PlatformDependencyChecker::isPresent);
            if (rule.isRequired() && !satisfied) {
                logger.severe(PREFIX + WonderLogMessages.MISSING_PLATFORM_DEPENDENCY.format(manifest.getName(), SUFFIX, rule.describe()));
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
        boolean existed = folder.exists();
        if (!existed) {
            folder.mkdirs();
        }

        if (!existed) {
            BundledContentExtractor.extract(context.getPlugin(), logger, "expansions", folder, "Expansions");
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
        if (value == null || value.trim().isEmpty()) {
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