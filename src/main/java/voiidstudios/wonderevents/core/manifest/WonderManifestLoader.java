package voiidstudios.wonderevents.core.manifest;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads {@link WonderManifest} objects from jars.
 */
public final class WonderManifestLoader {

    private WonderManifestLoader() {
    }

    public static WonderManifest load(File jarFile, YALogger logger) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(WonderManifest.FILE_NAME);
            if (entry == null) {
                if (logger != null) {
                    logger.passiveWarning("[WonderEvents] " + jarFile.getName() + " does not contain " + WonderManifest.FILE_NAME);
                }
                return null;
            }

            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
                return parse(yaml, jarFile.getName());
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.passiveWarning("[WonderEvents] Could not read " + jarFile.getName() + ": " + e.getMessage());
            }
            return null;
        }
    }

    public static WonderManifest parse(YamlConfiguration yaml, String sourceName) {
        String name = yaml.getString("name", sourceName.replaceFirst(".[Jj][Aa][Rr]$", ""));
        String id = yaml.getString("id", name);
        String version = yaml.getString("version", "0.0.0");
        String author = yaml.getString("author", "Unknown");
        String description = yaml.getString("description", "");
        String bootstrap = yaml.getString("bootstrap", yaml.getString("main", ""));
        String minVersion = yaml.getString("requirements.min_version", "0.0.0");

        Map<String, WonderManifest.DependencyRule> plugins = parseDependencies(yaml.getConfigurationSection("dependencies.plugins"));
        Map<String, WonderManifest.DependencyRule> expansions = parseDependencies(yaml.getConfigurationSection("dependencies.expansions"));
        Map<String, WonderManifest.DependencyRule> addons = parseDependencies(yaml.getConfigurationSection("dependencies.addons"));
        Map<String, WonderManifest.CommandDefinition> commands = parseCommands(yaml.getConfigurationSection("commands"));
        Map<String, WonderManifest.PermissionDefinition> permissions = parsePermissions(yaml.getConfigurationSection("permissions"));

        return new WonderManifest(
                id,
                name,
                version,
                author,
                description,
                bootstrap,
                minVersion,
                plugins,
                expansions,
                addons,
                commands,
                permissions
        );
    }

    private static Map<String, WonderManifest.DependencyRule> parseDependencies(ConfigurationSection section) {
        Map<String, WonderManifest.DependencyRule> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            boolean required = true;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                required = child != null && child.getBoolean("required", true);
            } else {
                required = section.getBoolean(key, true);
            }
            result.put(key, new WonderManifest.DependencyRule(required));
        }
        return result;
    }

    private static Map<String, WonderManifest.CommandDefinition> parseCommands(ConfigurationSection section) {
        Map<String, WonderManifest.CommandDefinition> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection commandSection = section.getConfigurationSection(key);
            if (commandSection == null) {
                continue;
            }

            String description = commandSection.getString("description", "");
            String usage = commandSection.getString("usage", "/" + key);
            String permission = commandSection.getString("permission", "");
            String permissionMessage = commandSection.getString("permission-message", commandSection.getString("permission_message", ""));
            List<String> aliases = new ArrayList<>(commandSection.getStringList("aliases"));
            result.put(key.toLowerCase(java.util.Locale.ROOT), new WonderManifest.CommandDefinition(
                    key,
                    description,
                    usage,
                    aliases,
                    permission,
                    permissionMessage
            ));
        }
        return result;
    }

    private static Map<String, WonderManifest.PermissionDefinition> parsePermissions(ConfigurationSection section) {
        Map<String, WonderManifest.PermissionDefinition> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection permissionSection = section.getConfigurationSection(key);
            if (permissionSection == null) {
                continue;
            }

            String description = permissionSection.getString("description", "");
            String defaultValue = permissionSection.getString("default", "");
            Map<String, Boolean> children = parseChildren(permissionSection.getConfigurationSection("children"));
            result.put(key.toLowerCase(java.util.Locale.ROOT), new WonderManifest.PermissionDefinition(
                    key,
                    description,
                    defaultValue,
                    children
            ));
        }
        return result;
    }

    private static Map<String, Boolean> parseChildren(ConfigurationSection section) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            result.put(key, section.getBoolean(key, true));
        }
        return result;
    }
}
