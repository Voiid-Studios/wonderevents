package voiidstudios.wonderevents.core.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;

public final class FeatureClassLoader extends URLClassLoader {
    public FeatureClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public URL getResource(String name) {
        URL own = findResource(name);
        if (own != null) {
            return own;
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResource(name) : null;
    }

    public InputStream getResourceAsStream(String name) {
        URL own = findResource(name);
        if (own != null) {
            try {
                return own.openStream();
            } catch (IOException ignored) {}
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResourceAsStream(name) : null;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        URL own = findResource(name);
        if (own != null) {
            return Collections.enumeration(Collections.singletonList(own));
        }
        ClassLoader parent = getParent();
        return parent != null ? parent.getResources(name) : Collections.emptyEnumeration();
    }
}
