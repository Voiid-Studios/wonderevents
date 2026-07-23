package voiidstudios.wonderevents.expansions;

import voiidstudios.wonderevents.api.WEABootstrap;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

final class WonderExpansionEntry {

    private final WEABootstrap expansion;
    private final WonderFeatureContext context;
    private final URLClassLoader classLoader;
    private final WonderExpansionDescriptor descriptor;
    private final File sourceFile;

    WonderExpansionEntry(WEABootstrap expansion, WonderFeatureContext context, URLClassLoader classLoader, WonderExpansionDescriptor descriptor, File sourceFile) {
        this.expansion = expansion;
        this.context = context;
        this.classLoader = classLoader;
        this.descriptor = descriptor;
        this.sourceFile = sourceFile;
    }

    WEABootstrap getExpansion() {
        return expansion;
    }

    WonderFeatureContext getContext() {
        return context;
    }

    WonderExpansionDescriptor getDescriptor() {
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

