package voiidstudios.wonderevents.core.loader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FeatureExportRegistry {
    private final Map<FeatureClassLoader, Registration> registrations = new IdentityHashMap<>();
    private final Map<ClassLoader, Map<String, Class<?>>> targets = new IdentityHashMap<>();

    public synchronized void attach(ClassLoader loader, Map<String, Class<?>> cache) {
        if (targets.containsKey(loader)) {
            return;
        }
        Map<String, Class<?>> types = new LinkedHashMap<>();
        for (Registration registration : registrations.values()) {
            types.putAll(registration.types);
        }
        checkBundledClasses(loader, types);
        publish(cache, types);
        targets.put(loader, cache);
    }

    public synchronized void detach(ClassLoader loader) {
        Map<String, Class<?>> cache = targets.remove(loader);
        if (cache != null) {
            for (Registration registration : registrations.values()) {
                remove(cache, registration.types);
            }
        }
    }

    public synchronized void register(FeatureClassLoader loader, String owner, List<String> packages) throws Exception {
        if (registrations.containsKey(loader) || packages.isEmpty()) {
            return;
        }
        for (Registration existing : registrations.values()) {
            for (String pkg : packages) {
                for (String other : existing.packages) {
                    if (pkg.equals(other) || pkg.startsWith(other + ".") || other.startsWith(pkg + ".")) {
                        throw new IllegalArgumentException("Export '" + pkg + "' in " + owner + " overlaps an export owned by " + existing.owner);
                    }
                }
            }
        }

        Map<String, Class<?>> types = new LinkedHashMap<>();
        for (URL url : loader.getURLs()) {
            try (JarFile jar = new JarFile(new File(url.toURI()))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String path = entries.nextElement().getName();
                    if (!path.endsWith(".class") || path.startsWith("META-INF/") || path.endsWith("package-info.class") || path.equals("module-info.class")) {
                        continue;
                    }
                    String name = path.substring(0, path.length() - 6).replace('/', '.');
                    if (loader.isExported(name)) {
                        types.put(name, Class.forName(name, false, loader));
                    }
                }
            }
        }
        for (String pkg : packages) {
            if (types.keySet().stream().noneMatch(name -> name.startsWith(pkg + "."))) {
                throw new IllegalArgumentException("Exported package '" + pkg + "' has no classes in " + owner);
            }
        }

        List<Map<String, Class<?>>> published = new ArrayList<>();
        try {
            for (Map.Entry<ClassLoader, Map<String, Class<?>>> target : targets.entrySet()) {
                checkBundledClasses(target.getKey(), types);
                Map<String, Class<?>> cache = target.getValue();
                publish(cache, types);
                published.add(cache);
            }
        } catch (RuntimeException e) {
            for (Map<String, Class<?>> cache : published) {
                remove(cache, types);
            }
            throw e;
        }
        registrations.put(loader, new Registration(owner, packages, types));
    }

    public synchronized void unregister(FeatureClassLoader loader) {
        Registration registration = registrations.remove(loader);
        if (registration != null) {
            for (Map<String, Class<?>> cache : targets.values()) {
                remove(cache, registration.types);
            }
        }
    }

    public synchronized Class<?> resolve(String className) throws ClassNotFoundException {
        for (Registration registration : registrations.values()) {
            Class<?> type = registration.types.get(className);
            if (type != null) {
                return type;
            }
        }
        throw new ClassNotFoundException("No active feature exports " + className);
    }

    public synchronized boolean hasExports() {
        return !registrations.isEmpty();
    }

    public synchronized void close() {
        for (ClassLoader loader : new ArrayList<>(targets.keySet())) {
            detach(loader);
        }
        registrations.clear();
    }

    private static void publish(Map<String, Class<?>> cache, Map<String, Class<?>> types) {
        List<String> inserted = new ArrayList<>();
        try {
            for (Map.Entry<String, Class<?>> entry : types.entrySet()) {
                Class<?> existing = cache.putIfAbsent(entry.getKey(), entry.getValue());
                if (existing != null && existing != entry.getValue()) {
                    throw new IllegalArgumentException("Export class collision: " + entry.getKey());
                }
                if (existing == null) {
                    inserted.add(entry.getKey());
                }
            }
        } catch (RuntimeException e) {
            for (String name : inserted) {
                cache.remove(name, types.get(name));
            }
            throw e;
        }
    }

    private static void checkBundledClasses(ClassLoader loader, Map<String, Class<?>> types) {
        if (loader instanceof URLClassLoader) {
            for (String name : types.keySet()) {
                if (((URLClassLoader) loader).findResource(name.replace('.', '/') + ".class") != null) {
                    throw new IllegalArgumentException("Consumer already bundles exported class: " + name);
                }
            }
        }
    }

    private static void remove(Map<String, Class<?>> cache, Map<String, Class<?>> types) {
        types.forEach((name, type) -> cache.remove(name, type));
    }

    private static final class Registration {
        private final String owner;
        private final List<String> packages;
        private final Map<String, Class<?>> types;

        private Registration(String owner, List<String> packages, Map<String, Class<?>> types) {
            this.owner = owner;
            this.packages = Collections.unmodifiableList(new ArrayList<>(packages));
            this.types = types;
        }
    }
}
