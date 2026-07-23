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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class WonderManifestLoader {
    private WonderManifestLoader() {}

    public static WonderManifest load(File jarFile, YALogger logger) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(WonderManifest.FILE_NAME);
            if (entry == null) {
                if (logger != null) {
                    logger.passiveWarning(jarFile.getName() + " does not contain " + WonderManifest.FILE_NAME);
                }
                return null;
            }

            try (InputStream in = jar.getInputStream(entry); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
                return parse(yaml, jarFile.getName());
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.passiveWarning("Could not read " + jarFile.getName() + ": " + e.getMessage());
            }
            return null;
        } catch (Throwable e) {
            // A malformed wonder-manifest.yml (bad types, invalid YAML structure, etc.)
            // must never bring down the whole addon/expansion loading loop.
            if (logger != null) {
                logger.passiveWarning("Could not parse " + WonderManifest.FILE_NAME + " in " + jarFile.getName() + ": " + e.getMessage());
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
        String bootstrap = yaml.getString("bootstrap", "");

        String minVersion = yaml.getString("requirements.wonder_min_version", "");
        String maxVersion = yaml.getString("requirements.wonder_max_version", "");
        String mcMinVersion = yaml.getString("requirements.mc_min_version", "");
        String mcMaxVersion = yaml.getString("requirements.mc_max_version", "");

        List<WonderManifest.DependencyRule> plugins = parseDependencyList(yaml, "dependencies.plugins");
        List<WonderManifest.DependencyRule> platforms = parseDependencyList(yaml, "dependencies.platforms");
        List<WonderManifest.DependencyRule> expansions = parseDependencyList(yaml, "dependencies.expansions");
        List<WonderManifest.DependencyRule> addons = parseDependencyList(yaml, "dependencies.addons");
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
                maxVersion,
                mcMinVersion,
                mcMaxVersion,
                plugins,
                platforms,
                expansions,
                addons,
                commands,
                permissions
        );
    }

    private static List<WonderManifest.DependencyRule> parseDependencyList(YamlConfiguration yaml, String path) {
        List<WonderManifest.DependencyRule> result = new ArrayList<>();

        List<Map<?, ?>> rawList = yaml.getMapList(path);
        if (rawList == null || rawList.isEmpty()) {
            return result;
        }

        for (Map<?, ?> raw : rawList) {
            boolean required = toBoolean(raw.get("required"), true);
            List<String> any = toStringList(raw.get("any"));
            List<String> all = toStringList(raw.get("all"));
            List<String> none = toStringList(raw.get("none"));

            if (any.isEmpty() && all.isEmpty() && none.isEmpty()) {
                continue;
            }

            result.add(new WonderManifest.DependencyRule(required, any, all, none));
        }

        return result;
    }

    private static List<String> toStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    result.add(String.valueOf(entry));
                }
            }
            return result;
        }

        return new ArrayList<>(List.of(String.valueOf(value)));
    }

    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
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
