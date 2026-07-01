package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.api.WonderBootstrap;

import java.io.File;
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
    private final File sourceFile;

    MagicAddonEntry(
            WonderBootstrap addon,
            MagicAddonContext context,
            URLClassLoader classLoader,
            MagicAddonDescriptor descriptor,
            File sourceFile
    ) {
        this.addon = addon;
        this.context = context;
        this.classLoader = classLoader;
        this.descriptor = descriptor;
        this.sourceFile = sourceFile;
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

    File getSourceFile() {
        return sourceFile;
    }

    void closeClassLoader() {
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }
}
