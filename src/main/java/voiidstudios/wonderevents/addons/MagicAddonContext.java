package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.api.ZFCommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The context object passed to every addon during {@link MagicAddon#onLoad}.
 *
 * <p>Provides safe access to core infrastructure without exposing gameplay
 * internals.
 */
public final class MagicAddonContext {

    private final PluginContext pluginContext;
    private final MagicAddonDescriptor descriptor;
    private final ClassLoader addonClassLoader;
    private final File addonDataFolder;
    private final YALogger logger;

    /** Runtime registrations created by this addon and cleaned up on unload. */
    private final List<Listener> registeredListeners = new ArrayList<>();
    private final List<ZFCommand> registeredCommands = new ArrayList<>();

    /** Cached YAML files keyed by relative file name (e.g. {@code "config.yml"}). */
    private final Map<String, FileConfiguration> loadedConfigs = new HashMap<>();

    MagicAddonContext(
            PluginContext pluginContext,
            MagicAddonDescriptor descriptor,
            ClassLoader addonClassLoader,
            File addonDataFolder
    ) {
        this.pluginContext     = pluginContext;
        this.descriptor        = descriptor;
        this.addonClassLoader  = addonClassLoader;
        this.addonDataFolder   = addonDataFolder;
        this.logger            = pluginContext.getPlugin().getYALogger();
    }

    // -------------------------------------------------------------------------
    // Core access
    // -------------------------------------------------------------------------

    public PluginContext getPluginContext() {
        return pluginContext;
    }

    public JavaPlugin getPlugin() {
        return pluginContext.getPlugin();
    }

    public JavaPlugin getCore() {
        return pluginContext.getPlugin();
    }

    public MagicAddonDescriptor getDescriptor() {
        return descriptor;
    }

    // -------------------------------------------------------------------------
    // Data folder & config
    // -------------------------------------------------------------------------

    public File getAddonDataFolder() {
        if (!addonDataFolder.exists()) {
            addonDataFolder.mkdirs();
        }
        return addonDataFolder;
    }

    public FileConfiguration getConfig(String fileName) {
        File file = new File(getAddonDataFolder(), fileName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        loadedConfigs.put(fileName, cfg);
        return cfg;
    }

    public void saveConfig(String fileName) {
        FileConfiguration cfg = loadedConfigs.get(fileName);
        if (cfg == null) {
            logWarn("[" + descriptor.getName() + "] saveConfig: '" + fileName + "' todavia no estaba cargado");
            return;
        }
        File file = new File(getAddonDataFolder(), fileName);
        try {
            cfg.save(file);
        } catch (IOException e) {
            logError("[" + descriptor.getName() + "] No pude guardar la config '" + fileName + "': " + e.getMessage());
        }
    }

    public void saveResource(String resourcePath, boolean replace) {
        File target = new File(getAddonDataFolder(), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (InputStream in = addonClassLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logWarn("[" + descriptor.getName() + "] No encontre el recurso en el jar del addon: " + resourcePath);
                return;
            }

            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logError("[" + descriptor.getName() + "] No pude guardar el recurso '" + resourcePath + "': " + e.getMessage());
        }
    }

    public void reloadAddonConfigs() {
        for (String fileName : loadedConfigs.keySet()) {
            File file = new File(getAddonDataFolder(), fileName);
            loadedConfigs.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void addConfigDefault(String fileName, String path, Object value) {
        FileConfiguration cfg = loadedConfigs.computeIfAbsent(
                fileName,
                f -> YamlConfiguration.loadConfiguration(new File(getAddonDataFolder(), f))
        );
        cfg.addDefault(path, value);
        cfg.options().copyDefaults(true);
    }

    public File getPluginDataFolder() {
        File pluginFolder = pluginContext.getPlugin().getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        return pluginFolder;
    }

    public FileConfiguration getPluginConfig(String fileName) {
        File file = new File(getPluginDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    public void savePluginResource(String resourcePath, boolean replace) {
        File target = new File(getPluginDataFolder(), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (InputStream in = addonClassLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logWarn("[" + descriptor.getName() + "] No encontre el recurso en el jar del addon: " + resourcePath);
                return;
            }

            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logError("[" + descriptor.getName() + "] No pude guardar el recurso compartido '" + resourcePath + "': " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    public void logInfo(String message) { logger.info("[Addon | " + descriptor.getName() + "] " + message); }
    public void logPInfo(String message) { logger.passiveInfo("[Addon | " + descriptor.getName() + "] " + message); }
    public void logProcess(String message) { logger.process("[Addon | " + descriptor.getName() + "] " + message); }
    public void logSuccess(String message) { logger.success("[Addon | " + descriptor.getName() + "] " + message); }
    public void logFailure(String message) { logger.failure("[Addon | " + descriptor.getName() + "] " + message); }
    public void logWarn(String message) { logger.passiveWarning("[Addon | " + descriptor.getName() + "] " + message); }
    public void logError(String message) { logger.severe("[Addon | " + descriptor.getName() + "] " + message); }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public void registerListener(Listener listener) {
        if (listener == null) {
            return;
        }
        pluginContext.getPlugin()
                .getServer()
                .getPluginManager()
                .registerEvents(listener, pluginContext.getPlugin());
        registeredListeners.add(listener);
    }

    public void registerCommand(ZFCommand command) {
        if (command == null) {
            return;
        }
        pluginContext.getCommandManager().registerAddonCommand(command);
        registeredCommands.add(command);
    }

    void cleanupRuntimeRegistrations() {
        for (Listener listener : new ArrayList<>(registeredListeners)) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();

        for (ZFCommand command : new ArrayList<>(registeredCommands)) {
            pluginContext.getCommandManager().unregisterAddonCommand(command);
        }
        registeredCommands.clear();
        loadedConfigs.clear();
    }
}
