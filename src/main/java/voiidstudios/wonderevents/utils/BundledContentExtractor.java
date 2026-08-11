package voiidstudios.wonderevents.utils;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BundledContentExtractor {
    private BundledContentExtractor() {}

    public static void extract(WEBootstrap plugin, YALogger logger, String resourceFolder, File targetFolder, String logLabel) {
        File jarFile = plugin.getPluginJarFile();
        if (jarFile == null || !jarFile.isFile()) {
            return;
        }

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

                File target = new File(targetFolder, simpleName);

                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.success("[" + logLabel + "] Installed bundled file: " + simpleName);
                } catch (IOException e) {
                    logger.passiveWarning("[" + logLabel + "] Failed to install bundled file " + simpleName + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.passiveWarning("[" + logLabel + "] Could not read the plugin jar to install bundled content: " + e.getMessage());
        }
    }
}
