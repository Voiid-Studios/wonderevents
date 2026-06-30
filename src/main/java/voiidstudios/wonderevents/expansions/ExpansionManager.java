
package voiidstudios.wonderevents.expansions;

import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class ExpansionManager {
    private final PluginContext context;
    private final YALogger logger;
    private final File folder;
    private final Map<String, ExpansionDescriptor> loaded = new LinkedHashMap<>();

    public ExpansionManager(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
        this.folder = new File(context.getPlugin().getDataFolder(), "expansions");
    }

    public int loadExpansions() {
        ensureFolder();
        File[] jars = folder.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.passiveInfo("[Expansions] No encontre expansiones en expansions/");
            return 0;
        }
        int count = 0;
        for (File jar : jars) {
            ExpansionDescriptor descriptor = readDescriptor(jar);
            if (descriptor == null) {
                continue;
            }
            loaded.put(descriptor.getId().toLowerCase(), descriptor);
            logger.success("[Expansions] Expansion validada: " + descriptor);
            count++;
        }
        return count;
    }

    public void disableExpansions() {
        loaded.clear();
    }

    public List<ExpansionDescriptor> getLoadedDescriptors() {
        return new ArrayList<>(loaded.values());
    }

    public boolean isLoaded(String id) {
        return id != null && loaded.containsKey(id.toLowerCase());
    }

    private void ensureFolder() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private ExpansionDescriptor readDescriptor(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            ZipEntry entry = jar.getEntry("expansion.yml");
            if (entry == null) {
                logger.passiveWarning("[Expansions] " + jarFile.getName() + " no contiene expansion.yml");
                return null;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(jar.getInputStream(entry)));
            String id = yaml.getString("id", jarFile.getName().replace(".jar", ""));
            String name = yaml.getString("name", id);
            String version = yaml.getString("version", "0.0.0");
            String author = yaml.getString("author", "unknown");
            String main = yaml.getString("main", "");
            String dependsExpansion = yaml.getString("depends.expansion", null);
            String dependsMinVersion = yaml.getString("depends.minVersion", null);
            if (main.isEmpty()) {
                logger.passiveWarning("[Expansions] " + jarFile.getName() + " no define main");
                return null;
            }
            return new ExpansionDescriptor(id, name, version, author, main, dependsExpansion, dependsMinVersion);
        } catch (IOException e) {
            logger.passiveWarning("[Expansions] No pude leer " + jarFile.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
