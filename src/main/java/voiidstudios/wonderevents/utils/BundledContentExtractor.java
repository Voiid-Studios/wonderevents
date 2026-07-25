package voiidstudios.wonderevents.utils;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BundledContentExtractor {

    private static final String MARKER_FILE_NAME = ".bundled-installed";

    private BundledContentExtractor() {
    }

    public static void extract(WEBootstrap plugin, YALogger logger, String resourceFolder, File targetFolder, String logLabel) {
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            logger.passiveWarning("[" + logLabel + "] Could not create the folder " + targetFolder.getPath() + " to install bundled content.");
            return;
        }

        File jarFile = plugin.getPluginJarFile();
        if (jarFile == null || !jarFile.isFile()) {
            return;
        }

        File marker = new File(targetFolder, MARKER_FILE_NAME);
        Set<String> installed = readMarker(marker);
        boolean markerChanged = false;

        String prefix = resourceFolder.endsWith("/") ? resourceFolder : resourceFolder + "/";

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(".jar")) {
                    continue;
                }

                String simpleName = name.substring(prefix.length());
                if (simpleName.isEmpty() || simpleName.contains("/")) {
                    continue;
                }

                if (installed.contains(simpleName)) {
                    continue;
                }

                File target = new File(targetFolder, simpleName);

                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.success("[" + logLabel + "] Installed bundled file: " + simpleName);
                } catch (IOException e) {
                    logger.passiveWarning("[" + logLabel + "] Failed to install bundled file " + simpleName + ": " + e.getMessage());
                    continue;
                }

                installed.add(simpleName);
                markerChanged = true;
            }
        } catch (IOException e) {
            logger.passiveWarning("[" + logLabel + "] Could not read the plugin jar to install bundled content: " + e.getMessage());
            return;
        }

        if (markerChanged) {
            writeMarker(marker, installed);
        }
    }

    private static Set<String> readMarker(File marker) {
        Set<String> result = new LinkedHashSet<>();
        if (!marker.isFile()) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(marker.toPath())) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        } catch (IOException ignored) {}

        return result;
    }

    private static void writeMarker(File marker, Set<String> installed) {
        try {
            Files.write(marker.toPath(), installed);
        } catch (IOException ignored) {}
    }
}
