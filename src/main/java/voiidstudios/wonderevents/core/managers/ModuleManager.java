
package voiidstudios.wonderevents.core.managers;

import java.util.LinkedHashMap;
import java.util.Map;

import voiidstudios.wonderevents.core.PluginContext;

public final class ModuleManager {
    private final PluginContext context;
    private final Map<String, Object> modules = new LinkedHashMap<>();

    public ModuleManager(PluginContext context) {
        this.context = context;
    }

    public void register(String id, Object module) {
        if (id != null && module != null) modules.put(id.toLowerCase(), module);
    }

    public boolean isModuleLoaded(String id) {
        return id != null && modules.containsKey(id.toLowerCase());
    }

    public int getLoadedModuleCount() {
        return modules.size();
    }

    public PluginContext getContext() {
        return context;
    }
}
