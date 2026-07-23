package voiidstudios.wonderevents.core.bootstrap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.managers.AdventureManager;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WonderFeatureContext {
    private final PluginContext pluginContext;
    private final WonderManifest manifest;
    private final ClassLoader featureClassLoader;
    private final File dataFolder;
    private final YALogger logger;

    private final List<Listener> registeredListeners = new ArrayList<>();
    private final List<WEACommand> registeredCommands = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private final Map<String, FileConfiguration> loadedConfigs = new LinkedHashMap<>();

    public WonderFeatureContext(PluginContext pluginContext, WonderManifest manifest, ClassLoader featureClassLoader, File dataFolder) {
        this.pluginContext = pluginContext;
        this.manifest = manifest;
        this.featureClassLoader = featureClassLoader;
        this.dataFolder = dataFolder;
        this.logger = pluginContext.getPlugin().getYALogger().withName(manifest.getName());
    }

    public PluginContext getPluginContext() {
        return pluginContext;
    }

    public WEBootstrap getCore() {
        return pluginContext.getPlugin();
    }

    public JavaPlugin getPlugin() {
        return pluginContext.getPlugin();
    }

    public WonderManifest getManifest() {
        return manifest;
    }

    public ClassLoader getFeatureClassLoader() {
        return featureClassLoader;
    }

    public File getDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    public File getFeatureDataFolder() {
        return getDataFolder();
    }

    public YALogger getLogger() {
        return logger;
    }

    public CommandSender getConsoleSender() {
        return pluginContext.getPlugin().getServer().getConsoleSender();
    }

    public AdventureManager getAdventure() {
        return pluginContext.getAdventureManager();
    }

    public FileConfiguration loadConfig(String relativePath) {
        File file = new File(getDataFolder(), relativePath);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        loadedConfigs.put(relativePath, configuration);
        return configuration;
    }

    public void saveConfig(String relativePath) {
        FileConfiguration configuration = loadedConfigs.get(relativePath);
        if (configuration == null) {
            logger.passiveWarning("saveConfig called before loadConfig for '" + relativePath + "'");
            return;
        }

        File file = new File(getDataFolder(), relativePath);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try {
            configuration.save(file);
        } catch (IOException e) {
            logger.severe("Could not save '" + relativePath + "': " + e.getMessage());
        }
    }

    public void reloadLoadedConfigs() {
        for (String path : new ArrayList<>(loadedConfigs.keySet())) {
            File file = new File(getDataFolder(), path);
            loadedConfigs.put(path, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void addDefault(String relativePath, String path, Object value) {
        FileConfiguration configuration = loadedConfigs.computeIfAbsent(relativePath, p -> YamlConfiguration.loadConfiguration(new File(getDataFolder(), p)));
        configuration.addDefault(path, value);
        configuration.options().copyDefaults(true);
    }

    public void saveResource(String resourcePath, boolean replace) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (InputStream in = featureClassLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.passiveWarning("Resource not found in feature jar: " + resourcePath);
                return;
            }

            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.severe("Could not copy resource '" + resourcePath + "': " + e.getMessage());
        }
    }

    public void registerListener(Listener listener) {
        if (listener == null) {
            return;
        }
        pluginContext.getPlugin().getServer().getPluginManager().registerEvents(listener, pluginContext.getPlugin());
        registeredListeners.add(listener);
    }

    public void registerCommand(WEACommand command) {
        if (command == null) {
            return;
        }
        pluginContext.getCommandManager().registerAddonCommand(command, manifest);
        registeredCommands.add(command);
    }

    public void registerPermission(Permission permission) {
        if (permission == null) {
            return;
        }

        PluginManager pluginManager = pluginContext.getPlugin().getServer().getPluginManager();
        if (pluginManager.getPermission(permission.getName()) != null) {
            logger.passiveWarning("Permission already exists and will not be registered twice: " + permission.getName());
            return;
        }

        pluginManager.addPermission(permission);
        registeredPermissions.add(permission);
    }

    public void unregisterRuntime() {
        PluginManager pluginManager = pluginContext.getPlugin().getServer().getPluginManager();

        for (Listener listener : new ArrayList<>(registeredListeners)) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();

        for (WEACommand command : new ArrayList<>(registeredCommands)) {
            pluginContext.getCommandManager().unregisterAddonCommand(command);
        }
        registeredCommands.clear();

        for (Permission permission : new ArrayList<>(registeredPermissions)) {
            try {
                pluginManager.removePermission(permission);
            } catch (Exception ignored) {}
        }
        registeredPermissions.clear();

        loadedConfigs.clear();
    }
}