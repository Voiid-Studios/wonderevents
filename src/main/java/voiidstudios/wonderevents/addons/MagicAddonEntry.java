package voiidstudios.wonderevents.addons;

import java.net.URLClassLoader;
import java.io.IOException;

/**
 * Internal runtime record that keeps a loaded addon instance, its context,
 * and the class-loader together.
 *
 * <p>This is not part of the public addon API — addons never receive one of
 * these directly.
 */
final class MagicAddonEntry {

    private final MagicAddon           addon;
    private final MagicAddonContext    context;
    private final URLClassLoader     classLoader;
    private final MagicAddonDescriptor descriptor;

    MagicAddonEntry(
            MagicAddon           addon,
            MagicAddonContext    context,
            URLClassLoader     classLoader,
            MagicAddonDescriptor descriptor
    ) {
        this.addon       = addon;
        this.context     = context;
        this.classLoader = classLoader;
        this.descriptor  = descriptor;
    }

    MagicAddon           getAddon()       { return addon; }
    MagicAddonContext    getContext()      { return context; }
    MagicAddonDescriptor getDescriptor()  { return descriptor; }

    /** Closes the addon's URLClassLoader quietly (best-effort). */
    void closeClassLoader() {
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }
}
