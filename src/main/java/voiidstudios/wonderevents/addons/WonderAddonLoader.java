package voiidstudios.wonderevents.addons;

import dev.faststats.Attributes;

import voiidstudios.wonderevents.api.WEABootstrap;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.core.manifest.WonderManifestLoader;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.loader.FeatureClassLoader;
import voiidstudios.wonderevents.core.metrics.MetricsManager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

final class WonderAddonLoader {
    private final PluginContext context;
    private final YALogger logger;

    WonderAddonLoader(PluginContext context) {
        this.context = context;
        this.logger = context.getPlugin().getYALogger();
    }

    WonderAddonEntry load(File jarFile) {
        WonderManifest manifest = WonderManifestLoader.load(jarFile, logger);
        if (manifest == null) {
            return null;
        }
        return load(jarFile, manifest);
    }

    WonderAddonEntry load(File jarFile, WonderManifest manifest) {
        URLClassLoader classLoader;
        try {
            classLoader = new FeatureClassLoader(new URL[]{jarFile.toURI().toURL()}, context.getPlugin().getClass().getClassLoader());
        } catch (Throwable e) {
            logger.passiveWarning("[Addons] Could not create the ClassLoader for " + manifest.getName() + ": " + e.getMessage());
            track(e, "classloader-creation", manifest);
            return null;
        }

        WEABootstrap addon;
        try {
            Class<?> mainClass = classLoader.loadClass(manifest.getBootstrap());
            if (!WEABootstrap.class.isAssignableFrom(mainClass)) {
                logger.passiveWarning("[Addons] The " + manifest.getName() + " Bootstrap class does not extend WEABootstrap");
                closeQuietly(classLoader);
                return null;
            }
            addon = (WEABootstrap) mainClass.getDeclaredConstructor().newInstance();
        } catch (LinkageError e) {
            logger.passiveSevere("[Addons] Could not start " + manifest.getName() + ": it looks like it was built against a different/older WonderEvents API (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). Ask the addon developer to recompile it against this WonderEvents version.");
            track(e, "bootstrap-instantiation", manifest);
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.passiveWarning("[Addons] Could not instantiate the bootstrap class of " + manifest.getName() + ": " + e.getMessage());
            track(e, "bootstrap-instantiation", manifest);
            closeQuietly(classLoader);
            return null;
        }

        File addonDataFolder = new File(new File(context.getPlugin().getDataFolder(), "addons"), manifest.getId());
        addonDataFolder.mkdirs();

        WonderAddonContext addonContext = new WonderAddonContext(context, manifest, classLoader, addonDataFolder);

        try {
            addon.init(addonContext);
        } catch (LinkageError e) {
            logger.passiveSevere("[Addons] Could not start " + manifest.getName() + ": it looks like it was built against a different/older WonderEvents API (" + e.getClass().getSimpleName() + ": " + e.getMessage() + "). Ask the addon developer to recompile it against this WonderEvents version.");
            track(e, "init", manifest);
            closeQuietly(classLoader);
            return null;
        } catch (Throwable e) {
            logger.passiveWarning("[Addons] Failed to initialize " + manifest.getName() + ": init() threw an error. Please contact the addon developer. Details: " + e.getMessage());
            track(e, "init", manifest);
            closeQuietly(classLoader);
            return null;
        }

        return new WonderAddonEntry(addon, addonContext, classLoader, new WonderAddonDescriptor(manifest), jarFile);
    }

    private void track(Throwable e, String stage, WonderManifest manifest) {
        MetricsManager.getErrorTracker().trackError(e)
                .attributes(Attributes.empty()
                        .put("component", "addon-loader")
                        .put("stage", stage)
                        .put("addon", manifest.getId())
                        .put("addon_version", manifest.getVersion()))
                .handled(true);
    }

    private void closeQuietly(URLClassLoader cl) {
        try {
            cl.close();
        } catch (Throwable ignored) {}
    }
}
