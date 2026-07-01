package voiidstudios.wonderevents.core.manifest;

import java.util.Collections;
import java.util.LinkedHashMap;
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
            Map<String, DependencyRule> addonDependencies
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

    private static Map<String, DependencyRule> immutableCopy(Map<String, DependencyRule> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
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
}
