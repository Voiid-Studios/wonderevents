package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.api.WonderBootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.log.YALogger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Builds the addon runtime entry after the manager has already decided that the
 * manifest is loadable.
 */
final class MagicAddonLoader {

    private final PluginContext context;
    private final YALogger logger;

    MagicAddonLoader(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
    }

    MagicAddonEntry load(File jarFile) {
        WonderManifest manifest = WonderManifestLoader.load(jarFile, logger);
        if (manifest == null) {
            return null;
        }
        return load(jarFile, manifest);
    }

    MagicAddonEntry load(File jarFile, WonderManifest manifest) {
        URLClassLoader classLoader;
        try {
            classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    context.getPlugin().getClass().getClassLoader()
            );
        } catch (Exception e) {
            logger.passiveWarning("[Addons] No pude crear el ClassLoader para " + manifest.getName() + ": " + e.getMessage());
            return null;
        }

        WonderBootstrap addon;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WonderBootstrap.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Addons] La clase bootstrap de " + manifest.getName() + " no extiende WonderBootstrap.");
                closeQuietly(classLoader);
                return null;
            }
            addon = (WonderBootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.passiveWarning("[Addons] No pude instanciar la clase bootstrap de " + manifest.getName() + ": " + e.getMessage());
            closeQuietly(classLoader);
            return null;
        }

        File addonDataFolder = new File(new File(context.getPlugin().getDataFolder(), "addons"), manifest.getId());
        addonDataFolder.mkdirs();

        MagicAddonContext addonContext = new MagicAddonContext(
                context,
                manifest,
                classLoader,
                addonDataFolder
        );

        addon.init(addonContext);
        return new MagicAddonEntry(addon, addonContext, classLoader, new MagicAddonDescriptor(manifest), jarFile);
    }

    private void closeQuietly(URLClassLoader cl) {
        try {
            cl.close();
        } catch (Exception ignored) {
        }
    }
}
