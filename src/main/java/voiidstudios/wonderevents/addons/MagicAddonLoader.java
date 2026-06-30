package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads a single addon jar file, reads its {@code addon.yml}, validates the
 * metadata, instantiates the main class, and returns a ready-to-wire
 * {@link MagicAddonEntry}.
 *
 * <p>Every jar gets its own {@link URLClassLoader}. If anything goes wrong,
 * a warning is logged and {@code null} is returned — the main plugin is never
 * crashed by a bad addon.
 */
final class MagicAddonLoader {

    private static final String DESCRIPTOR_FILE = "addon.yml";

    private final PluginContext context;
    private final YALogger      logger;

    MagicAddonLoader(PluginContext context) {
        this.context = context;
        this.logger  = context.getPlugin().getYALogger();
    }

    /**
     * Attempts to load the addon in {@code jarFile}.
     *
     * @param jarFile the addon jar to load
     * @return a loaded entry, or {@code null} if loading failed
     */
    MagicAddonEntry load(File jarFile) {
        // ── 1. Read addon.yml from the jar ───────────────────────────────────
        MagicAddonDescriptor descriptor;
        try {
            descriptor = readDescriptor(jarFile);
        } catch (Exception e) {
            logger.passiveWarning("[Addons] Saltando " + jarFile.getName() + "; no pude leer " + DESCRIPTOR_FILE + ": " + e.getMessage());
            return null;
        }

        if (descriptor == null) {
            logger.passiveWarning("[Addons] Saltando " + jarFile.getName() + "; no encontre " + DESCRIPTOR_FILE + " dentro del jar");
            return null;
        }

        // ── 2. Validate required fields ───────────────────────────────────────
        if (!validate(descriptor, jarFile.getName())) {
            return null;
        }

        // ── 3. Build a dedicated ClassLoader ─────────────────────────────────
        URLClassLoader classLoader;
        try {
            classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    context.getPlugin().getClass().getClassLoader()
            );
        } catch (Exception e) {
            logger.passiveWarning("[Addons] No pude crear el ClassLoader para " + descriptor.getName() + ": " + e.getMessage());
            return null;
        }

        // ── 4. Instantiate the main class ─────────────────────────────────────
        MagicAddon addon;
        try {
            Class<?> mainClass = classLoader.loadClass(descriptor.getMain());
            if (!MagicAddon.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Addons] La clase principal de " + descriptor.getName() + " no extiende D3VAddon. La omitire");
                closeQuietly(classLoader);
                return null;
            }
            addon = (MagicAddon) mainClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.passiveWarning("[Addons] No pude instanciar la clase principal de " + descriptor.getName() + ": " + e.getMessage());
            closeQuietly(classLoader);
            return null;
        }

        // ── 5. Build the data folder and context ─────────────────────────────
        File addonsBaseFolder = new File(context.getPlugin().getDataFolder(), "addons");
        File addonDataFolder  = new File(addonsBaseFolder, descriptor.getName());

        MagicAddonContext addonContext = new MagicAddonContext(
                context,
                descriptor,
                classLoader,
                addonDataFolder
        );


        addon.init(addonContext, descriptor);

        return new MagicAddonEntry(addon, addonContext, classLoader, descriptor);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private MagicAddonDescriptor readDescriptor(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(DESCRIPTOR_FILE);
            if (entry == null) {
                return null;
            }

            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                var yaml = YamlConfiguration.loadConfiguration(reader);

                String name       = yaml.getString("name");
                String version    = yaml.getString("version");
                String main       = yaml.getString("main");
                String description = yaml.getString("description");
                String author     = yaml.getString("author");
                String apiVersion = yaml.getString("api-version");
                List<String> depends     = yaml.getStringList("depends");
                List<String> softDepends = yaml.getStringList("soft-depends");

                return new MagicAddonDescriptor(
                        name, version, main, description, author, apiVersion,
                        depends, softDepends
                );
            }
        }
    }

    private boolean validate(MagicAddonDescriptor d, String fileName) {
        if (d.getName() == null || d.getName().isBlank()) {
            logger.passiveWarning("[Addons] Saltando " + fileName + ": falta 'name' en addon.yml");
            return false;
        }
        if (d.getVersion() == null || d.getVersion().isBlank()) {
            logger.passiveWarning("[Addons] Saltando " + fileName + ": falta 'version' en addon.yml");
            return false;
        }
        if (d.getMain() == null || d.getMain().isBlank()) {
            logger.passiveWarning("[Addons] Saltando " + fileName + ": falta 'main' en addon.yml");
            return false;
        }
        return true;
    }

    private void closeQuietly(URLClassLoader cl) {
        try {
            cl.close();
        } catch (IOException ignored) {
        }
    }
}
