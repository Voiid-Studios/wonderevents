package voiidstudios.wonderevents.core.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;

/**
 * ClassLoader used for expansions and addons.
 *
 * <p>Regular {@link URLClassLoader} uses parent-first delegation for BOTH
 * classes and resources. That is fine (and desired) for classes, since
 * expansions/addons need to see the core API classes from the main plugin.
 *
 * <p>However, parent-first delegation for <b>resources</b> is dangerous here:
 * if the main WonderEvents jar and a feature jar both ship a resource with
 * the same name (e.g. {@code config.yml}), a parent-first lookup will always
 * resolve to the main plugin's resource, never the feature's own one. This
 * previously caused expansions to have the core plugin's {@code config.yml}
 * copied into their own data folder instead of their own bundled config.
 *
 * <p>This loader keeps parent-first behaviour for {@link #loadClass}, but
 * makes resource lookups ({@link #getResource}, {@link #getResourceAsStream},
 * {@link #getResources}) self-first: the feature's own jar is checked before
 * falling back to the parent.
 */
public final class FeatureClassLoader extends URLClassLoader {

    public FeatureClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public URL getResource(String name) {
        URL own = findResource(name);
        if (own != null) {
            return own;
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResource(name) : null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL own = findResource(name);
        if (own != null) {
            try {
                return own.openStream();
            } catch (IOException ignored) {
                // fall through to parent lookup below
            }
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResourceAsStream(name) : null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        URL own = findResource(name);
        if (own != null) {
            return Collections.enumeration(Collections.singletonList(own));
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResources(name) : Collections.emptyEnumeration();
    }
}
