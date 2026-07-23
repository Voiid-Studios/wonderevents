package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.expansions.WonderExpansionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class WonderAddonContext extends WonderFeatureContext {
    public WonderAddonContext(PluginContext pluginContext, WonderManifest manifest, ClassLoader addonClassLoader, File addonDataFolder) {
        super(pluginContext, manifest, addonClassLoader, addonDataFolder);
    }

    public File getAddonDataFolder() {
        return getDataFolder();
    }

    public File getPluginDataFolder() {
        File pluginFolder = getPluginContext().getPlugin().getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        return pluginFolder;
    }

    public File getExpansionDataFolder(String expansionId) {
        WonderExpansionManager expansionManager = getPluginContext().getExpansionManager();
        if (expansionManager == null) {
            throw new IllegalStateException("ExpansionManager is not available");
        }

        if (!expansionManager.isLoaded(expansionId)) {
            getAddonLogger().passiveWarning("[" + getManifest().getName() + "] Writing to expansion folder '" + expansionId + "', but that expansion is not currently loaded.");
        }

        return expansionManager.getExpansionDataFolder(expansionId);
    }

    public FileConfiguration getExpansionConfig(String expansionId, String fileName) {
        return YamlConfiguration.loadConfiguration(new File(getExpansionDataFolder(expansionId), fileName));
    }

    public void saveExpansionConfig(String expansionId, String fileName, FileConfiguration configuration) {
        if (configuration == null) {
            getAddonLogger().passiveWarning("[" + getManifest().getName() + "] saveExpansionConfig called with a null configuration for '" + fileName + "'");
            return;
        }

        File file = new File(getExpansionDataFolder(expansionId), fileName);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try {
            configuration.save(file);
        } catch (IOException e) {
            getAddonLogger().severe("[" + getManifest().getName() + "] Could not save '" + fileName + "' in expansion '" + expansionId + "': " + e.getMessage());
        }
    }

    public void saveExpansionResource(String expansionId, String resourcePath, boolean replace) {
        File target = new File(getExpansionDataFolder(expansionId), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (InputStream in = getFeatureClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                getAddonLogger().passiveWarning("[" + getManifest().getName() + "] Could not find resource '" + resourcePath + "' inside the addon.");
                return;
            }

            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            getAddonLogger().severe("[" + getManifest().getName() + "] Could not copy resource '" + resourcePath + "' to expansion '" + expansionId + "': " + e.getMessage());
        }
    }

    public FileConfiguration getConfig(String fileName) {
        return loadConfig(fileName);
    }

    public void saveConfig(String fileName) {
        super.saveConfig(fileName);
    }

    public void reloadAddonConfigs() {
        reloadLoadedConfigs();
    }

    public void addConfigDefault(String fileName, String path, Object value) {
        FileConfiguration configuration = loadConfig(fileName);
        configuration.addDefault(path, value);
        configuration.options().copyDefaults(true);
    }

    public FileConfiguration getPluginConfig(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(getPluginDataFolder(), fileName));
    }

    public void savePluginResource(String resourcePath, boolean replace) {
        File target = new File(getPluginDataFolder(), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (java.io.InputStream in = getFeatureClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                getAddonLogger().passiveWarning("[" + getManifest().getName() + "] Could not find shared resource: " + resourcePath);
                return;
            }

            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            java.nio.file.Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            getAddonLogger().severe("[" + getManifest().getName() + "] Could not save shared resource '" + resourcePath + "': " + e.getMessage());
        }
    }

    public YALogger getAddonLogger() {
        return getLogger();
    }

    public JavaPlugin getPluginInstance() {
        return getPlugin();
    }
}
