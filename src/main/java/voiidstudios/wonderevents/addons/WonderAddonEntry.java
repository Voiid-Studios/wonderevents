package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.api.WEABootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

final class WonderAddonEntry {
    private final WEABootstrap addon;
    private final WonderAddonContext context;
    private final URLClassLoader classLoader;
    private final WonderAddonDescriptor descriptor;
    private final File sourceFile;

    WonderAddonEntry(WEABootstrap addon, WonderAddonContext context, URLClassLoader classLoader, WonderAddonDescriptor descriptor, File sourceFile) {
        this.addon = addon;
        this.context = context;
        this.classLoader = classLoader;
        this.descriptor = descriptor;
        this.sourceFile = sourceFile;
    }

    WEABootstrap getAddon() {
        return addon;
    }

    WonderAddonContext getContext() {
        return context;
    }

    WonderAddonDescriptor getDescriptor() {
        return descriptor;
    }

    File getSourceFile() {
        return sourceFile;
    }

    void closeClassLoader() {
        try {
            classLoader.close();
        } catch (IOException ignored) {}
    }
}
