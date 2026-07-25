package voiidstudios.wonderevents.core.metrics;

import dev.faststats.ErrorTracker;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.data.Metric;

import voiidstudios.wonderevents.addons.WonderAddonDescriptor;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.expansions.WonderExpansionDescriptor;
import voiidstudios.wonderevents.utils.DownloadSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MetricsManager {
    private static final String PROJECT_TOKEN = "ead0df10ff128bd5370f44d02521e473";

    private static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            .ignoreError(ClassNotFoundException.class);

    private final PluginContext context;
    private final long startTime = System.currentTimeMillis();
    private BukkitContext bukkitContext;
    private String downloadSource;

    public MetricsManager(PluginContext context) {
        this.context = context;
    }

    public void start() {
        if (bukkitContext != null) {
            return;
        }

        downloadSource = DownloadSource.detect(context.getPlugin());

        ERROR_TRACKER.getAttributes()
                .put("download_source", downloadSource);

        bukkitContext = new BukkitContext.Factory(context.getPlugin(), PROJECT_TOKEN)
                .errorTrackerService(ERROR_TRACKER)
                .metrics(factory -> factory
                        .addMetric(Metric.numberMap("expansions", this::buildExpansionChart))
                        .addMetric(Metric.numberMap("addons", this::buildAddonChart))
                        .addMetric(Metric.string("download_source", () -> downloadSource))
                        .addMetric(Metric.number("uptime_days", () -> (System.currentTimeMillis() - startTime) / (1000L * 60 * 60 * 24)))
                        .create())
                .create();

        bukkitContext.ready();
    }

    public void stop() {
        if (bukkitContext != null) {
            bukkitContext.shutdown();
            bukkitContext = null;
        }
    }

    public static ErrorTracker getErrorTracker() {
        return ERROR_TRACKER;
    }

    private Map<String, Integer> buildExpansionChart() {
        if (context.getExpansionManager() == null) {
            return Map.of();
        }
        return buildFlattened(
                context.getExpansionManager().getLoadedDescriptors(),
                WonderExpansionDescriptor::getName,
                WonderExpansionDescriptor::getVersion
        );
    }

    private Map<String, Integer> buildAddonChart() {
        if (context.getAddonManager() == null) {
            return Map.of();
        }
        return buildFlattened(
                context.getAddonManager().getLoadedDescriptors(),
                WonderAddonDescriptor::getName,
                WonderAddonDescriptor::getVersion
        );
    }

    private <T> Map<String, Integer> buildFlattened(List<T> descriptors, Function<T, String> nameGetter, Function<T, String> versionGetter) {
        Map<String, Integer> data = new LinkedHashMap<>();
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

            String key = name + "::" + version;
            data.merge(key, 1, Integer::sum);
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