
package voiidstudios.wonderevents.expansions;

public final class ExpansionDescriptor {
    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String mainClass;
    private final String dependsExpansion;
    private final String dependsMinVersion;

    public ExpansionDescriptor(String id, String name, String version, String author, String mainClass, String dependsExpansion, String dependsMinVersion) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.mainClass = mainClass;
        this.dependsExpansion = dependsExpansion;
        this.dependsMinVersion = dependsMinVersion;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getMainClass() { return mainClass; }
    public String getDependsExpansion() { return dependsExpansion; }
    public String getDependsMinVersion() { return dependsMinVersion; }

    @Override
    public String toString() {
        return name + " (" + version + ")";
    }
}
