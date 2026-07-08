package voiidstudios.wonderevents.core.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.WEBootstrap;

import java.io.File;

public final class ConfigManager {
    private final WEBootstrap plugin;
    private final File configFile;
    private FileConfiguration config;

    public ConfigManager(WEBootstrap plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void bootstrap() {
        ensureFolders();
        ensureCoreConfig();
        reload();
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void ensureFolders() {
        File data = plugin.getDataFolder();
        if (!data.exists()) {
            data.mkdirs();
        }
        new File(data, "expansions").mkdirs();
        new File(data, "addons").mkdirs();
        new File(data, "messages").mkdirs();
        new File(data, "messages/custom").mkdirs();
        new File(data, "messages/origins").mkdirs();
    }

    private void ensureCoreConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("Config.faststats_metrics", true);
    }

    public String getLanguage() {
        return config.getString("Messages.language", "en_US");
    }

    public File getConfigFile() {
        return configFile;
    }
}
