package voiidstudios.wonderevents.core.manifest;

/**
 * Resolves {@code dependencies.platforms} entries.
 *
 * <p>Platform entries are fully-qualified class names (e.g.
 * {@code io.papermc.paper.configuration.Configuration}) that are expected to be present or
 * absent on the running server, plus a couple of convenience aliases for common platforms.
 */
public final class PlatformDependencyChecker {

    private PlatformDependencyChecker() {
    }

    public static boolean isPresent(String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }

        switch (entry.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "paper":
                return hasClass("io.papermc.paper.configuration.Configuration")
                        || hasClass("com.destroystokyo.paper.PaperConfig");
            case "spigot":
                return hasClass("org.spigotmc.SpigotConfig");
            case "bukkit":
                return hasClass("org.bukkit.Bukkit");
            default:
                return hasClass(entry.trim());
        }
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className, false, PlatformDependencyChecker.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
