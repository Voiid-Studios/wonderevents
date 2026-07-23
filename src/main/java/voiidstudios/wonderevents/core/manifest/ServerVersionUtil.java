package voiidstudios.wonderevents.core.manifest;

import org.bukkit.Bukkit;

public final class ServerVersionUtil {
    private static volatile String cachedVersion;

    private ServerVersionUtil() {}

    public static String getMinecraftVersion() {
        String cached = cachedVersion;
        if (cached != null) {
            return cached;
        }

        String resolved = resolve();
        cachedVersion = resolved;
        return resolved;
    }

    private static String resolve() {
        try {
            Class.forName("io.papermc.paper.ServerBuildInfo");
            return modernVersion();
        } catch (ClassNotFoundException ignored) {
            return legacyVersion();
        }
    }

    private static String modernVersion() {
        try {
            io.papermc.paper.ServerBuildInfo info = io.papermc.paper.ServerBuildInfo.buildInfo();
            String mcVersion = info.minecraftVersionName();
            if (mcVersion != null && !mcVersion.isBlank()) {
                return mcVersion.trim();
            }
        } catch (Throwable ignored) {}
        return legacyVersion();
    }

    private static String legacyVersion() {
        try {
            String bukkitVersion = Bukkit.getBukkitVersion(); // e.g. "1.8.8-R0.1-SNAPSHOT"
            String mcVersion = bukkitVersion.split("-")[0];
            if (!mcVersion.isBlank()) {
                return mcVersion.trim();
            }
        } catch (Throwable ignored) {}
        return "0.0.0";
    }
}
