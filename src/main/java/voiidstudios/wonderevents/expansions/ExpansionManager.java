package voiidstudios.wonderevents.expansions;

import org.bukkit.Bukkit;

import voiidstudios.wonderevents.api.WonderBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.manifest.VersionUtil;

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
            logger.passiveInfo("[Expansions] No encontre expansiones en expansions/");
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
                    logger.passiveWarning("[Expansions] " + jar.getName() + " no define bootstrap.");
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
                    logger.passiveWarning("[Expansions] Ya existe una expansion con el id '" + expansionId + "'. Omito " + jar.getName());
                    continue;
                }

                ExpansionEntry entry = loadEntry(jar, manifest);
                if (entry == null) {
                    continue;
                }

                try {
                    entry.getContext().saveResource("config.yml", false);
                    entry.getContext().saveResource("data/base.yml", false);
                    entry.getExpansion().onLoad(entry.getContext());
                    loaded.put(expansionId, entry);
                    ensureExpansionFolder(entry.getDescriptor());
                    logger.success("[Expansions] Expansion despierta: " + entry.getDescriptor());
                    loadedNow++;
                    progress = true;
                } catch (Exception e) {
                    logger.passiveWarning("[Expansions] onLoad() fallo en la expansion '" + expansionId + "': " + e.getMessage());
                    entry.closeClassLoader();
                }
            }

            pending = nextPending;
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            for (File jar : pending) {
                logger.passiveWarning("[Expansions] Sigo sin poder cargar " + jar.getName() + " porque le faltan dependencias.");
            }
        }

        return loadedNow;
    }

    public void enableExpansions() {
        for (ExpansionEntry entry : loaded.values()) {
            try {
                entry.getExpansion().onEnable();
            } catch (Exception e) {
                logger.passiveWarning("[Expansions] onEnable() fallo en la expansion '" + entry.getDescriptor().getName() + "': " + e.getMessage());
            }
        }
    }

    public int reloadExpansions() {
        for (ExpansionEntry entry : new ArrayList<>(loaded.values())) {
            try {
                entry.getExpansion().onReload();
            } catch (Exception e) {
                logger.passiveWarning("[Expansions] onReload() fallo en la expansion '" + entry.getDescriptor().getName() + "': " + e.getMessage());
            }
        }

        disableExpansions();
        int loadedCount = loadExpansions();
        enableExpansions();
        logger.success("[Expansions] Recarga completada. Expansiones activas: " + loadedCount);
        return loadedCount;
    }

    public void disableExpansions() {
        List<ExpansionEntry> entries = new ArrayList<>(loaded.values());
        Collections.reverse(entries);

        for (ExpansionEntry entry : entries) {
            String name = entry.getDescriptor().getName();
            try {
                entry.getExpansion().onDisable();
            } catch (Exception e) {
                logger.passiveWarning("[Expansions] onDisable() fallo en la expansion '" + name + "': " + e.getMessage());
            }

            try {
                entry.getContext().unregisterRuntime();
            } catch (Exception cleanupError) {
                logger.passiveWarning("[Expansions] La limpieza fallo en la expansion '" + name + "': " + cleanupError.getMessage());
            }

            entry.closeClassLoader();
            logger.passiveInfo("[Expansions] Expansion dormida: " + name);
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
            classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    context.getPlugin().getClass().getClassLoader()
            );
        } catch (Exception e) {
            logger.passiveWarning("[Expansions] No pude crear el ClassLoader para " + manifest.getName() + ": " + e.getMessage());
            return null;
        }

        WonderBootstrap expansion;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WonderBootstrap.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Expansions] La clase bootstrap de " + manifest.getName() + " no extiende WonderBootstrap.");
                closeQuietly(classLoader);
                return null;
            }
            expansion = (WonderBootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.passiveWarning("[Expansions] No pude instanciar la clase bootstrap de " + manifest.getName() + ": " + e.getMessage());
            closeQuietly(classLoader);
            return null;
        }

        WonderFeatureContext featureContext = new WonderFeatureContext(
                context,
                manifest,
                classLoader,
                getExpansionDataFolder(manifest.getId())
        );

        expansion.init(featureContext);
        return new ExpansionEntry(expansion, featureContext, classLoader, new ExpansionDescriptor(manifest));
    }

    private LoadDecision canLoad(WonderManifest manifest) {
        String required = manifest.getMinCoreVersion();
        if (required != null && !required.isBlank()) {
            String current = VersionUtil.normalize(context.getPlugin().getDescription().getVersion());
            if (!VersionUtil.isAtLeast(current, required)) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requiere WonderEvents >= " + required + " y tienes " + current);
                return LoadDecision.REJECTED;
            }
        }

        for (var entry : manifest.getPluginDependencies().entrySet()) {
            boolean present = Bukkit.getPluginManager().getPlugin(entry.getKey()) != null;
            if (entry.getValue().isRequired() && !present) {
                logger.passiveWarning("[Expansions] " + manifest.getName() + " requiere el plugin " + entry.getKey() + " y no esta presente.");
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
        File dataFolder = getExpansionDataFolder(descriptor);
        new File(dataFolder, "data").mkdirs();
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
