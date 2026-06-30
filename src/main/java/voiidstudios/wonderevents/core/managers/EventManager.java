
package voiidstudios.wonderevents.core.managers;

import org.bukkit.event.Listener;

import voiidstudios.wonderevents.core.PluginContext;

import java.util.ArrayList;
import java.util.List;

public final class EventManager {
    private final PluginContext context;
    private final List<Listener> listeners = new ArrayList<>();

    public EventManager(PluginContext context) {
        this.context = context;
    }

    public void registerListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    public int getRegisteredListenerCount() {
        return listeners.size();
    }

    public PluginContext getContext() {
        return context;
    }
}
