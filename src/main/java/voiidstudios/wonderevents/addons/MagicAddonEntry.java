package voiidstudios.wonderevents.addons;

import org.bukkit.configuration.file.YamlConfiguration;

import voiidstudios.wonderevents.api.WonderBootstrap;

import java.io.IOException;
import java.net.URLClassLoader;

/**
 * Internal runtime record that keeps a loaded addon instance, its context, and
 * the class-loader together.
 */
final class MagicAddonEntry {

    private final WonderBootstrap addon;
    private final MagicAddonContext context;
    private final URLClassLoader classLoader;
    private final MagicAddonDescriptor descriptor;

    MagicAddonEntry(
            WonderBootstrap addon,
            MagicAddonContext context,
            URLClassLoader classLoader,
            MagicAddonDescriptor descriptor
    ) {
        this.addon = addon;
        this.context = context;
        this.classLoader = classLoader;
        this.descriptor = descriptor;
    }

    WonderBootstrap getAddon() {
        return addon;
    }

    MagicAddonContext getContext() {
        return context;
    }

    MagicAddonDescriptor getDescriptor() {
        return descriptor;
    }

    void closeClassLoader() {
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }
}
