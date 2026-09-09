package voiidstudios.wonderevents.addons;

import dev.faststats.Attributes;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.WonderLogMessages;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;
import voiidstudios.wonderevents.core.manifest.ServerVersionUtil;
import voiidstudios.wonderevents.core.manifest.PlatformDependencyChecker;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.utils.BundledContentExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class WonderAddonManager {
    private static final String PREFIX = "[Addons] ";
    private static final String SUFFIX = "addon";

    private final PluginContext context;
    private final YALogger logger;
    private final File addonsFolder;

    private final Map<String, WonderAddonEntry> loaded = new LinkedHashMap<>();
    private final java.util.Set<String> disabled = new java.util.LinkedHashSet<>();

    public WonderAddonManager(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
        this.addonsFolder = new File(context.getPlugin().getDataFolder(), "addons");
    }

    public int loadAddons() {
        ensureAddonsFolder();

        File[] jars = addonsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return 0;
        }

        List<File> pending = new ArrayList<>();
        Collections.addAll(pending, jars);

        WonderAddonLoader loader = new WonderAddonLoader(context);
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

                String addonId = manifest.getId().toLowerCase();
                if (loaded.containsKey(addonId)) {
                    logger.warning(PREFIX + WonderLogMessages.ALREADY_LOADED.format(addonId, loaded.get(addonId).getDescriptor().getVersion(), SUFFIX, jar.getName()));
                    continue;
                }

                WonderAddonEntry entry = loader.load(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getAddon().onLoad(entry.getContext());
                    loaded.put(addonId, entry);
                    logger.success(PREFIX + "Loaded addon: " + entry.getDescriptor());
                    loadedIds.add(addonId);
                    progress = true;
                } catch (Throwable e) {
                    logger.severe(PREFIX + WonderLogMessages.ONLOAD_ERROR.format(jar.getName(), SUFFIX));
                    track(e, "onLoad", addonId);
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

        return loadedIds.size();
    }

    public void enableAddons() {
        for (WonderAddonEntry entry : loaded.values()) {
            try {
                entry.getAddon().onEnable();
            } catch (Throwable e) {
                logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                logger.severe(e.getMessage());
                track(e, "onEnable", entry.getDescriptor().getName());
            }
        }
    }

    public int reloadAddons() {
        ensureAddonsFolder();

        Map<String, File> currentJars = discoverAddonJars();
        List<String> previousOrder = new ArrayList<>(loaded.keySet());

        disableMissingAddons(currentJars);

        java.util.Set<String> newlyLoaded = loadNewAddons(currentJars);
        reloadPresentAddons(currentJars, previousOrder, newlyLoaded);

        logger.success(PREFIX + "Reloaded! Active addons: " + loaded.size());
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
                WonderManifest existingManifest = WonderManifestLoader.load(jars.get(id), logger);
                String existingVersion = existingManifest != null ? existingManifest.getVersion() : "?";
                logger.warning(PREFIX + WonderLogMessages.ALREADY_LOADED.format(id, existingVersion, SUFFIX, file.getName()));
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

            WonderAddonEntry entry = loaded.remove(id);
            this.disabled.remove(id);
            if (entry == null) {
                continue;
            }

            String name = entry.getDescriptor().getName();
            try {
                entry.getAddon().onDisable();
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
            logger.passiveInfo(PREFIX + "Unloaded addon: " + name);
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

        WonderAddonLoader loader = new WonderAddonLoader(context);
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

                String addonId = manifest.getId().toLowerCase();
                if (loaded.containsKey(addonId)) {
                    continue;
                }

                WonderAddonEntry entry = loader.load(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getAddon().onLoad(entry.getContext());
                    loaded.put(addonId, entry);
                    loadedIds.add(addonId);
                    try {
                        entry.getAddon().onEnable();
                    } catch (Throwable enableError) {
                        logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                        logger.severe(enableError.getMessage());
                        track(enableError, "onEnable", entry.getDescriptor().getName());
                    }

                    logger.success(PREFIX + "Loaded addon: " + entry.getDescriptor());
                    progress = true;
                } catch (Throwable e) {
                    logger.severe(PREFIX + WonderLogMessages.ONLOAD_ERROR.format(addonId, SUFFIX));
                    logger.severe(e.getMessage());
                    track(e, "onLoad", addonId);
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

    private int reloadPresentAddons(Map<String, File> currentJars, List<String> previousOrder, java.util.Set<String> newlyLoaded) {
        int reloaded = 0;
        for (String id : previousOrder) {
            if (!currentJars.containsKey(id) || newlyLoaded.contains(id)) {
                continue;
            }

            WonderAddonEntry entry = loaded.get(id);
            if (entry == null) {
                continue;
            }

            try {
                entry.getAddon().onReload();
                reloaded++;
            } catch (Throwable e) {
                logger.severe(PREFIX + WonderLogMessages.ONRELOAD_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
                logger.severe(e.getMessage());
                track(e, "onReload", entry.getDescriptor().getName());
            }
        }
        return reloaded;
    }

    public void disableAddons() {
        List<WonderAddonEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (WonderAddonEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getAddon().onDisable();
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
            logger.passiveInfo(PREFIX + "Unloaded addon: " + name);
        }

        loaded.clear();
        disabled.clear();
    }

    public List<WonderAddonDescriptor> getLoadedDescriptors() {
        List<WonderAddonDescriptor> list = new ArrayList<>();
        for (WonderAddonEntry entry : loaded.values()) {
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

    public boolean isEnabled(String id) {
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        return loaded.containsKey(normalized) && !disabled.contains(normalized);
    }

    public AddonToggleResult enableAddon(String id) {
        if (id == null) {
            return AddonToggleResult.NOT_FOUND;
        }

        String normalized = id.toLowerCase();
        WonderAddonEntry entry = loaded.get(normalized);
        if (entry == null) {
            return AddonToggleResult.NOT_FOUND;
        }

        if (!disabled.contains(normalized)) {
            return AddonToggleResult.ALREADY;
        }

        try {
            entry.getAddon().onEnable();
        } catch (Throwable e) {
            logger.severe(PREFIX + WonderLogMessages.ONENABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "onEnable", entry.getDescriptor().getName());
        }

        disabled.remove(normalized);
        logger.passiveInfo(PREFIX + "Enabled addon: " + entry.getDescriptor().getName());
        return AddonToggleResult.SUCCESS;
    }

    public AddonToggleResult disableAddon(String id) {
        if (id == null) {
            return AddonToggleResult.NOT_FOUND;
        }

        String normalized = id.toLowerCase();
        WonderAddonEntry entry = loaded.get(normalized);
        if (entry == null) {
            return AddonToggleResult.NOT_FOUND;
        }

        if (disabled.contains(normalized)) {
            return AddonToggleResult.ALREADY;
        }

        try {
            entry.getAddon().onDisable();
        } catch (Throwable e) {
            logger.severe(PREFIX + WonderLogMessages.ONDISABLE_ERROR.format(entry.getDescriptor().getName(), SUFFIX));
            logger.severe(e.getMessage());
            track(e, "onDisable", entry.getDescriptor().getName());
        }

        disabled.add(normalized);
        logger.passiveInfo(PREFIX + "Disabled addon: " + entry.getDescriptor().getName());
        return AddonToggleResult.SUCCESS;
    }

    public enum AddonToggleResult {
        SUCCESS,
        ALREADY,
        NOT_FOUND
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
            boolean satisfied = rule.isSatisfiedBy(name -> context.getPlugin().getServer().getPluginManager().getPlugin(name) != null);
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
            boolean satisfied = rule.isSatisfiedBy(id -> context.getExpansionManager() != null && context.getExpansionManager().isLoaded(id));
            if (rule.isRequired() && !satisfied) {
                return LoadDecision.RETRY_LATER;
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getAddonDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(this::isLoaded);
            if (rule.isRequired() && !satisfied) {
                return LoadDecision.RETRY_LATER;
            }
        }

        return LoadDecision.READY;
    }

    private void registerManifestPermissions(WonderAddonContext featureContext, WonderManifest manifest) {
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

    private String describeMissingDependency(WonderManifest manifest) {
        for (WonderManifest.DependencyRule rule : manifest.getExpansionDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(id -> context.getExpansionManager() != null && context.getExpansionManager().isLoaded(id));
            if (rule.isRequired() && !satisfied) {
                return "expansion " + rule.describe();
            }
        }

        for (WonderManifest.DependencyRule rule : manifest.getAddonDependencies()) {
            boolean satisfied = rule.isSatisfiedBy(this::isLoaded);
            if (rule.isRequired() && !satisfied) {
                return "addon " + rule.describe();
            }
        }

        return "an unknown dependency";
    }

    private void track(Throwable e, String stage, String addonId) {
        MetricsManager.getErrorTracker().trackError(e)
                .attributes(Attributes.empty()
                        .put("component", "addon-manager")
                        .put("stage", stage)
                        .put("addon", addonId))
                .handled(true);
    }

    private void ensureAddonsFolder() {
        boolean existed = addonsFolder.exists();
        if (!existed) {
            addonsFolder.mkdirs();
        }

        if (!existed) {
            BundledContentExtractor.extract(context.getPlugin(), logger, "addons", addonsFolder, "Addons");
        }
    }

    private enum LoadDecision {
        READY,
        RETRY_LATER,
        REJECTED
    }
}
