package voiidstudios.wonderevents.addons;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the metadata found in an addon's {@code addon.yml}.
 *
 * <p>Required fields: {@code name}, {@code version}, {@code main}.
 * All other fields are optional and fall back to safe defaults.
 */
public final class MagicAddonDescriptor {

    private final String name;
    private final String version;
    private final String main;
    private final String description;
    private final String author;
    private final String apiVersion;
    private final List<String> depends;
    private final List<String> softDepends;

    public MagicAddonDescriptor(
            String name,
            String version,
            String main,
            String description,
            String author,
            String apiVersion,
            List<String> depends,
            List<String> softDepends
    ) {
        this.name        = name;
        this.version     = version;
        this.main        = main;
        this.description = description == null ? "" : description;
        this.author      = author      == null ? "" : author;
        this.apiVersion  = apiVersion  == null ? "" : apiVersion;
        this.depends     = depends     == null ? Collections.emptyList() : List.copyOf(depends);
        this.softDepends = softDepends == null ? Collections.emptyList() : List.copyOf(softDepends);
    }

    /** The addon's unique display name (e.g. {@code "MyAddon"}). */
    public String getName()        { return name; }

    /** The addon version string (e.g. {@code "1.0.0"}). */
    public String getVersion()     { return version; }

    /** Fully-qualified class name of the addon main class. */
    public String getMain()        { return main; }

    /** Human-readable description of what the addon does. */
    public String getDescription() { return description; }

    /** Addon author name. */
    public String getAuthor()      { return author; }

    /** Minimum D3V-ZE API version required by this addon. */
    public String getApiVersion()  { return apiVersion; }

    /** Hard dependencies (addon names that must be loaded first). */
    public List<String> getDepends()     { return depends; }

    /** Soft dependencies (loaded first if available, but not required). */
    public List<String> getSoftDepends() { return softDepends; }

    @Override
    public String toString() {
        return name + " v" + version + " by " + (author.isBlank() ? "Unknown" : author);
    }
}
