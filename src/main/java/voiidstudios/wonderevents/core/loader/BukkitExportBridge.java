package voiidstudios.wonderevents.core.loader;

import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Map;

public final class BukkitExportBridge implements Listener {
    private final JavaPlugin core;
    private final FeatureExportRegistry registry;

    public BukkitExportBridge(JavaPlugin core, FeatureExportRegistry registry) {
        this.core = core;
        this.registry = registry;
        core.getServer().getPluginManager().registerEvents(this, core);
    }

    public void refresh() {
        for (Plugin plugin : core.getServer().getPluginManager().getPlugins()) {
            attach(plugin);
        }
    }

    public void onPluginEnable(PluginEnableEvent event) {
        if (!registry.hasExports()) {
            return;
        }
        try {
            attach(event.getPlugin());
        } catch (IllegalStateException e) {
            core.getLogger().warning("[Exports] " + e.getMessage());
        }
    }

    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == core) {
            return;
        }
        registry.detach(event.getPlugin().getClass().getClassLoader());
    }

    private void attach(Plugin plugin) {
        ClassLoader loader = plugin.getClass().getClassLoader();
        if (!loader.getClass().getName().equals("org.bukkit.plugin.java.PluginClassLoader")) {
            return;
        }
        try {
            Field field = loader.getClass().getDeclaredField("classes");
            field.setAccessible(true);
            Object value = field.get(loader);
            if (!(value instanceof Map)) {
                throw new IllegalStateException("PluginClassLoader.classes is not a map");
            }
            registry.attach(loader, (Map<String, Class<?>>) value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot publish feature exports to " + plugin.getName() + ": " + e.getMessage(), e);
        }
    }
}
