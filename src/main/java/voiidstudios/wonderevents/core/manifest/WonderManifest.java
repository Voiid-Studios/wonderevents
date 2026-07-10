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

    private final List<DependencyRule> pluginDependencies;
    private final List<DependencyRule> platformDependencies;
    private final List<DependencyRule> expansionDependencies;
    private final List<DependencyRule> addonDependencies;
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
            List<DependencyRule> pluginDependencies,
            List<DependencyRule> platformDependencies,
            List<DependencyRule> expansionDependencies,
            List<DependencyRule> addonDependencies,
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
        this.pluginDependencies = immutableList(pluginDependencies);
        this.platformDependencies = immutableList(platformDependencies);
        this.expansionDependencies = immutableList(expansionDependencies);
        this.addonDependencies = immutableList(addonDependencies);
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

    public List<DependencyRule> getPluginDependencies() {
        return pluginDependencies;
    }

    public List<DependencyRule> getPlatformDependencies() {
        return platformDependencies;
    }

    public List<DependencyRule> getExpansionDependencies() {
        return expansionDependencies;
    }

    public List<DependencyRule> getAddonDependencies() {
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
        private final List<String> any;
        private final List<String> all;
        private final List<String> none;

        public DependencyRule(boolean required, List<String> any, List<String> all, List<String> none) {
            this.required = required;
            this.any = immutableList(any);
            this.all = immutableList(all);
            this.none = immutableList(none);
        }

        public boolean isRequired() {
            return required;
        }

        public List<String> getAny() {
            return any;
        }

        public List<String> getAll() {
            return all;
        }

        public List<String> getNone() {
            return none;
        }

        /**
         * Evaluates this rule using the given presence checker, which reports whether a
         * given entry (a plugin name, class name, expansion id, etc.) is currently present.
         *
         * <p>Rules with no {@code any}/{@code all}/{@code none} entries are trivially satisfied.
         */
        public boolean isSatisfiedBy(java.util.function.Predicate<String> presence) {
            if (!all.isEmpty()) {
                for (String entry : all) {
                    if (!presence.test(entry)) {
                        return false;
                    }
                }
            }

            if (!any.isEmpty()) {
                boolean matched = false;
                for (String entry : any) {
                    if (presence.test(entry)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }

            if (!none.isEmpty()) {
                for (String entry : none) {
                    if (presence.test(entry)) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * Human-readable description of this rule's conditions, for logging purposes.
         */
        public String describe() {
            List<String> parts = new ArrayList<>();
            if (!all.isEmpty()) {
                parts.add("all of " + all);
            }
            if (!any.isEmpty()) {
                parts.add("any of " + any);
            }
            if (!none.isEmpty()) {
                parts.add("none of " + none);
            }
            return String.join(" and ", parts);
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

    private static <T> List<T> immutableList(List<T> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(input));
    }
}
