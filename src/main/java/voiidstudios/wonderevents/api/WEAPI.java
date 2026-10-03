package voiidstudios.wonderevents.api;

import voiidstudios.wonderevents.core.PluginContext;

public final class WEAPI {
    private final PluginContext context;

    public WEAPI(PluginContext context) {
        this.context = context;
    }

    public PluginContext getContext() {
        return context;
    }

    public Class<?> resolveExportedClass(String className) throws ClassNotFoundException {
        return context.getExportRegistry().resolve(className);
    }
}
