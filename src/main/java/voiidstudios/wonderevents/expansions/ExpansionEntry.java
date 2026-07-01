package voiidstudios.wonderevents.expansions;

import voiidstudios.wonderevents.api.WonderBootstrap;
import voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext;

import java.io.IOException;
import java.net.URLClassLoader;

/**
 * Internal runtime record that keeps a loaded expansion instance, its context,
 * and the class-loader together.
 */
final class ExpansionEntry {

    private final WonderBootstrap expansion;
    private final WonderFeatureContext context;
    private final URLClassLoader classLoader;
    private final ExpansionDescriptor descriptor;

    ExpansionEntry(
            WonderBootstrap expansion,
            WonderFeatureContext context,
            URLClassLoader classLoader,
            ExpansionDescriptor descriptor
    ) {
        this.expansion = expansion;
        this.context = context;
        this.classLoader = classLoader;
        this.descriptor = descriptor;
    }

    WonderBootstrap getExpansion() {
        return expansion;
    }

    WonderFeatureContext getContext() {
        return context;
    }

    ExpansionDescriptor getDescriptor() {
        return descriptor;
    }

    void closeClassLoader() {
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }
}
