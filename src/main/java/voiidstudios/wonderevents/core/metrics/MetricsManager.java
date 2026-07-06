package voiidstudios.wonderevents.core.metrics;

import voiidstudios.wonderevents.addons.MagicAddonDescriptor;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.expansions.ExpansionDescriptor;
import voiidstudios.wonderevents.libs.bstats.Metrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MetricsManager {
    private static final int SERVICE_ID = 32296;

    private final PluginContext context;
    private Metrics metrics;

    public MetricsManager(PluginContext context) {
        this.context = context;
    }

    public void start() {
        if (metrics != null) {
            return;
        }

        metrics = new Metrics(context.getPlugin(), SERVICE_ID);
        metrics.addCustomChart(new Metrics.DrilldownPie("expansions", this::buildExpansionChart));
        metrics.addCustomChart(new Metrics.DrilldownPie("addons", this::buildAddonChart));
    }

    public void stop() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    private Map<String, Map<String, Integer>> buildExpansionChart() {
        if (context.getExpansionManager() == null) {
            return Map.of();
        }
        return buildDrilldown(
                context.getExpansionManager().getLoadedDescriptors(),
                ExpansionDescriptor::getName,
                ExpansionDescriptor::getVersion
        );
    }

    private Map<String, Map<String, Integer>> buildAddonChart() {
        if (context.getAddonManager() == null) {
            return Map.of();
        }
        return buildDrilldown(
                context.getAddonManager().getLoadedDescriptors(),
                MagicAddonDescriptor::getName,
                MagicAddonDescriptor::getVersion
        );
    }

    private <T> Map<String, Map<String, Integer>> buildDrilldown(
            List<T> descriptors,
            Function<T, String> nameGetter,
            Function<T, String> versionGetter
    ) {
        Map<String, Map<String, Integer>> data = new LinkedHashMap<>();
        if (descriptors == null || descriptors.isEmpty()) {
            return data;
        }

        for (T descriptor : descriptors) {
            if (descriptor == null) {
                continue;
            }

            String name = clean(nameGetter.apply(descriptor));
            String version = clean(versionGetter.apply(descriptor));
            if (name == null || version == null) {
                continue;
            }

            data.computeIfAbsent(name, ignored -> new LinkedHashMap<>())
                    .merge(version, 1, Integer::sum);
        }

        return data;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
