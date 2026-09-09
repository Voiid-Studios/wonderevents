package voiidstudios.wonderevents.core.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.WEBootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigManager {
    private static final int MIN_UPDATE_CHECK_DELAY_SECONDS = 300; // 5 minutes

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
        migrateConfig();
    }

    private void migrateConfig() {
        InputStream defStream = plugin.getResource("config.yml");
        if (defStream == null) {
            return;
        }

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));

        if (copyMissingKeys(defaults, config, "")) {
            save();
        }
    }

    private boolean copyMissingKeys(ConfigurationSection defaultSection, ConfigurationSection target, String path) {
        boolean changed = false;
        for (String key : defaultSection.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            Object value = defaultSection.get(key);

            if (value instanceof ConfigurationSection) {
                if (copyMissingKeys((ConfigurationSection) value, target, fullPath)) {
                    changed = true;
                }
            } else if (!target.isSet(fullPath)) {
                target.set(fullPath, value);
                changed = true;
            }
        }
        return changed;
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save updated config.yml: " + e.getMessage());
        }
    }

    private void ensureFolders() {
        File data = plugin.getDataFolder();
        if (!data.exists()) {
            data.mkdirs();
        }
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
        return config.getBoolean("Config.metrics", true);
    }

    public boolean isAutoUpdate() {
        return config.getBoolean("Config.auto_updater.download", true);
    }

    public boolean isUpdateNotification() {
        return config.getBoolean("Config.auto_updater.notify", true);
    }

    public int getUpdateCheckDelay() {
        int configured = config.getInt("Config.auto_updater.delay", 43200);
        return Math.max(configured, MIN_UPDATE_CHECK_DELAY_SECONDS);
    }

    public String getLanguage() {
        return config.getString("Messages.language", "en_US");
    }

    public File getConfigFile() {
        return configFile;
    }
}