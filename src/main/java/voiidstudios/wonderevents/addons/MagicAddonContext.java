package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;

/**
 * Addon-friendly extension of {@link WonderFeatureContext}.
 *
 * <p>It keeps the generic runtime helpers from the base context and adds a few
 * addon-specific aliases so older addon code keeps reading naturally.
 */
public class MagicAddonContext extends WonderFeatureContext {

    public MagicAddonContext(
            PluginContext pluginContext,
            WonderManifest manifest,
            ClassLoader addonClassLoader,
            File addonDataFolder
    ) {
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
                getAddonLogger().passiveWarning("[" + getManifest().getName() + "] No encontre el recurso compartido: " + resourcePath);
                return;
            }

            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            java.nio.file.Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            getAddonLogger().severe("[" + getManifest().getName() + "] No pude guardar el recurso compartido '" + resourcePath + "': " + e.getMessage());
        }
    }

    public YALogger getAddonLogger() {
        return getLogger();
    }

    public JavaPlugin getPluginInstance() {
        return getPlugin();
    }
}
