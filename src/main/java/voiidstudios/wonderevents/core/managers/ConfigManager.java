
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
        this.configFile = new File(new File(plugin.getDataFolder(), "configs"), "magic-config.yml");
        reload();
    }

    public void reload() {
        if (!configFile.exists()) {
            plugin.saveResource("configs/magic-config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isBstatsMetricsEnabled() {
        return config.getBoolean("Config.bstats_metrics", true);
    }

    public File getConfigFile() {
        return configFile;
    }
}
