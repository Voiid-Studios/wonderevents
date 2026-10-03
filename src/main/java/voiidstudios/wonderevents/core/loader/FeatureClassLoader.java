package voiidstudios.wonderevents.core.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FeatureClassLoader extends URLClassLoader {
    private final List<String> exports;
    private FeatureExportRegistry exportRegistry;

    public FeatureClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, Collections.emptyList());
    }

    public FeatureClassLoader(URL[] urls, ClassLoader parent, List<String> exports) {
        super(urls, parent);
        this.exports = validateExports(exports);
    }

    public static List<String> validateExports(List<String> packages) {
        Set<String> result = new LinkedHashSet<>();
        if (packages != null) {
            for (String entry : packages) {
                String name = entry == null ? "" : entry.trim();
                if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
                    throw new IllegalArgumentException("Invalid exported package: " + entry);
                }
                for (String reserved : new String[]{"java", "javax", "jdk", "sun", "org.bukkit", "net.minecraft", "io.papermc", "com.destroystokyo", "voiidstudios.wonderevents"}) {
                    if (name.equals(reserved) || name.startsWith(reserved + ".") || reserved.startsWith(name + ".")) {
                        throw new IllegalArgumentException("Cannot export reserved package: " + name);
                    }
                }
                result.add(name);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    public boolean isExported(String className) {
        for (String pkg : exports) {
            if (className.startsWith(pkg + ".")) {
                return true;
            }
        }
        return false;
    }

    public void setExportRegistry(FeatureExportRegistry registry) {
        this.exportRegistry = registry;
    }

    public void close() throws IOException {
        if (exportRegistry != null) {
            exportRegistry.unregister(this);
        }
        super.close();
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isExported(name)) {
            return super.loadClass(name, resolve);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null) {
                type = findClass(name);
            }
            if (resolve) {
                resolveClass(type);
            }
            return type;
        }
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
