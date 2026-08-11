package voiidstudios.wonderevents.update;

import voiidstudios.wonderevents.core.log.YALogger;

public final class UpdateDownloader {
    private final YALogger logger;

    public UpdateDownloader(YALogger logger, UpdateChecker updateChecker) {
        this.logger = logger;
    }

    public boolean downloadUpdate() {
        logger.failure("[Update] Failed to download update: Error 999 - Since you downloaded this plugin from CurseForge, we cannot automatically update your plugin due to the CurseForge's policies; we strongly recommend that you update it manually. <3");
        return false;
    }
}
