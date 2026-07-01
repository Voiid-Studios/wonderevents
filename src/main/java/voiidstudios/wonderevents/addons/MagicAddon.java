package voiidstudios.wonderevents.addons;

import voiidstudios.wonderevents.api.WonderBootstrap;

/**
 * Backwards-compatible addon base.
 *
 * <p>Addons can extend this class and keep the older {@code onLoad(context)}
 * style, but under the hood they are still loaded through the shared
 * WonderEvents bootstrap pipeline.
 */
public abstract class MagicAddon extends WonderBootstrap {

    @Override
    public final void onLoad(voiidstudios.wonderevents.core.bootstrap.WonderFeatureContext context) {
        if (context instanceof MagicAddonContext addonContext) {
            onLoad(addonContext);
            return;
        }
        onLoad(null);
    }

    /**
     * Addon-specific load hook. Override this instead of the generic one when
     * you need the addon helpers.
     */
    public void onLoad(MagicAddonContext context) {
        // default no-op
    }

    protected final MagicAddonContext getAddonContext() {
        return (MagicAddonContext) getFeatureContext();
    }
}
