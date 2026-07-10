package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.expansions.ExpansionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

    /**
     * Returns the data folder belonging to the given expansion (e.g. {@code expansions/Extremo3Vanilla/}),
     * so an addon can store its files there instead of creating its own {@code addons/<id>/} folder.
     *
     * <p>This is useful for addons that only make sense alongside a specific expansion and want to
     * share its file layout (e.g. a resource-pack add-on writing into the expansion it extends).
     * The folder is created if it does not exist yet, regardless of whether the expansion is
     * currently loaded.
     */
    public File getExpansionDataFolder(String expansionId) {
        ExpansionManager expansionManager = getPluginContext().getExpansionManager();
        if (expansionManager == null) {
            throw new IllegalStateException("ExpansionManager is not available");
        }

        if (!expansionManager.isLoaded(expansionId)) {
            getAddonLogger().passiveWarning("[" + getManifest().getName() + "] Escribiendo en la carpeta de la expansion '"
                    + expansionId + "', pero esa expansion no esta cargada actualmente.");
        }

        return expansionManager.getExpansionDataFolder(expansionId);
    }

    /**
     * Loads (or creates an empty) YAML file from the given expansion's data folder.
     */
    public FileConfiguration getExpansionConfig(String expansionId, String fileName) {
        return YamlConfiguration.loadConfiguration(new File(getExpansionDataFolder(expansionId), fileName));
    }

    /**
     * Saves a {@link FileConfiguration} into the given expansion's data folder.
     */
    public void saveExpansionConfig(String expansionId, String fileName, FileConfiguration configuration) {
        if (configuration == null) {
            getAddonLogger().passiveWarning("[" + getManifest().getName() + "] saveExpansionConfig llamado con una configuracion nula para '" + fileName + "'");
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
            getAddonLogger().severe("[" + getManifest().getName() + "] No pude guardar '" + fileName + "' en la expansion '" + expansionId + "': " + e.getMessage());
        }
    }

    /**
     * Copies a resource bundled in this addon's jar into the given expansion's data folder,
     * preserving any subfolder structure encoded in {@code resourcePath}.
     */
    public void saveExpansionResource(String expansionId, String resourcePath, boolean replace) {
        File target = new File(getExpansionDataFolder(expansionId), resourcePath);
        if (target.exists() && !replace) {
            return;
        }

        try (InputStream in = getFeatureClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                getAddonLogger().passiveWarning("[" + getManifest().getName() + "] No encontre el recurso '" + resourcePath + "' dentro del addon.");
                return;
            }

            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            getAddonLogger().severe("[" + getManifest().getName() + "] No pude copiar el recurso '" + resourcePath + "' hacia la expansion '" + expansionId + "': " + e.getMessage());
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
