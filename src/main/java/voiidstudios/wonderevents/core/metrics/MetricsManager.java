
package voiidstudios.wonderevents.core.metrics;

import org.bstats.bukkit.Metrics;

import voiidstudios.wonderevents.core.PluginContext;

public final class MetricsManager {
    private final PluginContext context;
    private Metrics metrics;

    public MetricsManager(PluginContext context) {
        this.context = context;
    }

    public void start() {
        if (metrics == null) {
            //metrics = new Metrics(context.getPlugin(), 32296);
        }
    }

    public void stop() {
        metrics = null;
    }
}
