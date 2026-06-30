package voiidstudios.wonderevents.api;

import voiidstudios.wonderevents.core.PluginContext;

public final class ZFAPI {

    private final PluginContext context;

    public ZFAPI(PluginContext context) {
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }
}
