package voiidstudios.wonderevents.expansions;

import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifest.DependencyRule;

import java.util.List;

/**
 * Immutable descriptor for an expansion loaded from {@code wonder-manifest.yml}.
 */
public final class ExpansionDescriptor {

    private final WonderManifest manifest;

    public ExpansionDescriptor(WonderManifest manifest) {
        this.manifest = manifest;
    }

    public WonderManifest getManifest() {
        return manifest;
    }

    public String getId() {
        return manifest.getId();
    }

    public String getName() {
        return manifest.getName();
    }

    public String getVersion() {
        return manifest.getVersion();
    }

    public String getAuthor() {
        return manifest.getAuthor();
    }

    public String getDescription() {
        return manifest.getDescription();
    }

    public String getBootstrap() {
        return manifest.getBootstrap();
    }

    public String getMinCoreVersion() {
        return manifest.getMinCoreVersion();
    }

    public List<DependencyRule> getPluginDependencies() {
        return manifest.getPluginDependencies();
    }

    public List<DependencyRule> getPlatformDependencies() {
        return manifest.getPlatformDependencies();
    }

    public List<DependencyRule> getExpansionDependencies() {
        return manifest.getExpansionDependencies();
    }

    public List<DependencyRule> getAddonDependencies() {
        return manifest.getAddonDependencies();
    }

    @Override
    public String toString() {
        String author = manifest.getAuthor().isBlank() ? "Unknown" : manifest.getAuthor();
        return manifest.getName() + " v" + manifest.getVersion() + " by " + author;
    }
}
