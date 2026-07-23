package voiidstudios.wonderevents.api;

import voiidstudios.wonderevents.core.PluginContext;

public interface WEAModule {
    String getName();

    boolean isEnabledByDefault();

    void onLoad(PluginContext context);

    void onEnable(PluginContext context);

    void onDisable(PluginContext context);
}
