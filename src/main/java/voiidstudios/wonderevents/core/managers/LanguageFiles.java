package voiidstudios.wonderevents.core.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class LanguageFiles {
    static final String DEFAULT_LANGUAGE = "en";

    private LanguageFiles() { }

    static File file(JavaPlugin plugin, String name) {
        if (name == null || !name.matches("[a-zA-Z0-9_-]+")) {
            return null;
        }
        return new File(plugin.getDataFolder(), "langs/" + name + ".yml");
    }

    static String resolve(JavaPlugin plugin, String name) {
        if (name == null || name.trim().isEmpty()) {
            name = DEFAULT_LANGUAGE;
        }
        name = name.trim();
        File exact = file(plugin, name);
        if (exact != null && exact.isFile()) {
            return name;
        }
        if (name.matches("(?i)[a-z]{2}(?:[_-][a-z0-9]{2,8})*")) {
            String language = name.substring(0, 2).toLowerCase(Locale.ROOT);
            File shortened = file(plugin, language);
            if (shortened != null && shortened.isFile()) {
                return language;
            }
        }
        return null;
    }

    static void installBundled(JavaPlugin plugin) {
        try {
            File source = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (source.isDirectory()) {
                File[] files = new File(source, "langs").listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            install(plugin, "langs/" + file.getName());
                        }
                    }
                }
            } else {
                try (JarFile jar = new JarFile(source)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        install(plugin, entries.nextElement().getName());
                    }
                }
            }
        } catch (IOException | URISyntaxException | RuntimeException e) {
            plugin.getLogger().warning("Could not install bundled languages: " + e.getMessage());
        }
    }

    private static void install(JavaPlugin plugin, String path) {
        if (!path.startsWith("langs/") || !path.endsWith(".yml")) {
            return;
        }
        String name = path.substring("langs/".length(), path.length() - ".yml".length());
        File target = file(plugin, name);
        if (target != null && !target.exists()) {
            plugin.saveResource(path, false);
        }
    }
}