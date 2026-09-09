package voiidstudios.wonderevents.core.discovery;

import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.WEBootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassDiscovery {
    private ClassDiscovery() {}

    public static List<Class<?>> findClasses(JavaPlugin plugin, String packageName) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        String packagePath = packageName.replace('.', '/');

        discoverFromCodeSource(plugin, packageName, packagePath, classes);
        discoverFromClassLoader(plugin, packageName, packagePath, classes);

        return new ArrayList<>(classes);
    }

    public static <T> T instantiate(Class<?> type, Class<T> expectedType, Object context) {
        try {
            Constructor<?> contextConstructor = findContextConstructor(type, context.getClass());
            Object instance;
            if (contextConstructor != null) {
                contextConstructor.setAccessible(true);
                instance = contextConstructor.newInstance(context);
            } else {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            }
            return expectedType.cast(instance);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create an instance of " + type.getName(), exception);
        }
    }

    private static Constructor<?> findContextConstructor(Class<?> type, Class<?> contextType) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(contextType)) {
                return constructor;
            }
        }
        return null;
    }

    private static void discoverFromCodeSource(JavaPlugin plugin, String packageName, String packagePath, Set<Class<?>> classes) {
        CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return;
        }

        File source = new File(codeSource.getLocation().getPath());
        if (source.isFile() && source.getName().endsWith(".jar")) {
            discoverFromJar(plugin, source, packageName, packagePath, classes);
            return;
        }

        if (source.isDirectory()) {
            discoverFromDirectory(plugin, new File(source, packagePath), packageName, classes);
        }
    }

    private static void discoverFromClassLoader(JavaPlugin plugin, String packageName, String packagePath, Set<Class<?>> classes) {
        try {
            Enumeration<URL> resources = plugin.getClass().getClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    File directory = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name()));
                    discoverFromDirectory(plugin, directory, packageName, classes);
                } else if ("jar".equals(resource.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) resource.openConnection();
                    discoverFromJar(plugin, new File(connection.getJarFileURL().getFile()), packageName, packagePath, classes);
                }
            }
        } catch (IOException exception) {
            logWarning(plugin, "Could not scan package " + packageName + ": " + exception.getMessage());
        }
    }

    private static void discoverFromJar(JavaPlugin plugin, File jarFile, String packageName, String packagePath, Set<Class<?>> classes) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(packagePath) || !name.endsWith(".class")) {
                    continue;
                }
                addClass(plugin, toClassName(name), classes);
            }
        } catch (IOException exception) {
            logWarning(plugin, "Could not read the jar to scan " + packageName + ": " + exception.getMessage());
        }
    }

    private static void discoverFromDirectory(JavaPlugin plugin, File directory, String packageName, Set<Class<?>> classes) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                discoverFromDirectory(plugin, file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                addClass(plugin, className, classes);
            }
        }
    }

    private static void addClass(JavaPlugin plugin, String className, Set<Class<?>> classes) {
        try {
            Class<?> type = Class.forName(className, false, plugin.getClass().getClassLoader());
            if (!type.isInterface() && !type.isAnnotation() && !type.isEnum()) {
                classes.add(type);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            plugin.getLogger().fine("Class ignored during discovery: " + className);
        }
    }

    private static void logWarning(JavaPlugin plugin, String message) {
        if (plugin instanceof WEBootstrap && ((WEBootstrap) plugin).getYALogger() != null) {
            ((WEBootstrap) plugin).getYALogger().warning(message);
            return;
        }

        plugin.getLogger().warning(message);
    }

    private static String toClassName(String entryName) {
        return entryName.replace('/', '.').replace(".class", "");
    }
}
