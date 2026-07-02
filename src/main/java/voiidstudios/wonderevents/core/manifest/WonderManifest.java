package voiidstudios.wonderevents.core.manifest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parsed contents of {@code wonder-manifest.yml}.
 *
 * <p>The same format is used by both expansions and addons.
 */
public final class WonderManifest {

    public static final String FILE_NAME = "wonder-manifest.yml";

    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String bootstrap;
    private final String minCoreVersion;

    private final Map<String, DependencyRule> pluginDependencies;
    private final Map<String, DependencyRule> expansionDependencies;
    private final Map<String, DependencyRule> addonDependencies;
    private final Map<String, CommandDefinition> commands;
    private final Map<String, PermissionDefinition> permissions;

    public WonderManifest(
            String id,
            String name,
            String version,
            String author,
            String description,
            String bootstrap,
            String minCoreVersion,
            Map<String, DependencyRule> pluginDependencies,
            Map<String, DependencyRule> expansionDependencies,
            Map<String, DependencyRule> addonDependencies,
            Map<String, CommandDefinition> commands,
            Map<String, PermissionDefinition> permissions
    ) {
        this.id = emptyIfNull(id);
        this.name = emptyIfNull(name);
        this.version = emptyIfNull(version);
        this.author = emptyIfNull(author);
        this.description = emptyIfNull(description);
        this.bootstrap = emptyIfNull(bootstrap);
        this.minCoreVersion = emptyIfNull(minCoreVersion);
        this.pluginDependencies = immutableCopy(pluginDependencies);
        this.expansionDependencies = immutableCopy(expansionDependencies);
        this.addonDependencies = immutableCopy(addonDependencies);
        this.commands = immutableCopy(commands);
        this.permissions = immutableCopy(permissions);
    }

    public String getId() {
        return id.isBlank() ? name : id;
    }

    public String getName() {
        return name.isBlank() ? getId() : name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public String getMinCoreVersion() {
        return minCoreVersion;
    }

    public Map<String, DependencyRule> getPluginDependencies() {
        return pluginDependencies;
    }

    public Map<String, DependencyRule> getExpansionDependencies() {
        return expansionDependencies;
    }

    public Map<String, DependencyRule> getAddonDependencies() {
        return addonDependencies;
    }

    public Map<String, CommandDefinition> getCommands() {
        return commands;
    }

    public Map<String, PermissionDefinition> getPermissions() {
        return permissions;
    }

    public CommandDefinition getCommand(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return commands.get(normalizeKey(name));
    }

    public PermissionDefinition getPermission(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return permissions.get(normalizeKey(name));
    }

    private static <T> Map<String, T> immutableCopy(Map<String, T> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    public static final class DependencyRule {
        private final boolean required;

        public DependencyRule(boolean required) {
            this.required = required;
        }

        public boolean isRequired() {
            return required;
        }
    }

    public static final class CommandDefinition {
        private final String name;
        private final String description;
        private final String usage;
        private final List<String> aliases;
        private final String permission;
        private final String permissionMessage;

        public CommandDefinition(String name, String description, String usage, List<String> aliases, String permission, String permissionMessage) {
            this.name = emptyIfNull(name);
            this.description = emptyIfNull(description);
            this.usage = emptyIfNull(usage);
            this.aliases = immutableList(aliases);
            this.permission = emptyIfNull(permission);
            this.permissionMessage = emptyIfNull(permissionMessage);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUsage() {
            return usage;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public String getPermission() {
            return permission;
        }

        public String getPermissionMessage() {
            return permissionMessage;
        }
    }

    public static final class PermissionDefinition {
        private final String name;
        private final String description;
        private final String defaultValue;
        private final Map<String, Boolean> children;

        public PermissionDefinition(String name, String description, String defaultValue, Map<String, Boolean> children) {
            this.name = emptyIfNull(name);
            this.description = emptyIfNull(description);
            this.defaultValue = emptyIfNull(defaultValue);
            this.children = immutableCopy(children);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public Map<String, Boolean> getChildren() {
            return children;
        }
    }

    private static List<String> immutableList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(input));
    }
}
