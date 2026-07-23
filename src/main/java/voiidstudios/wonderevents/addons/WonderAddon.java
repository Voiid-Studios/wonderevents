package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.api.WEABootstrap;

public abstract class WonderAddon extends WEABootstrap {
    public final void onLoad(voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext context) {
        if (context instanceof WonderAddonContext addonContext) {
            onLoad(addonContext);
            return;
        }
        onLoad(null);
    }

    public void onLoad(WonderAddonContext context) {}

    protected final WonderAddonContext getAddonContext() {
        return (WonderAddonContext) getFeatureContext();
    }
}
