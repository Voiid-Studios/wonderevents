package voiidstudios.wonderevents.core.manifest;

public final class PlatformDependencyChecker {
    private PlatformDependencyChecker() {}

    public static boolean isPresent(String entry) {
        if (entry == null || entry.isBlank()) return false;

        switch (entry.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "paper":
                return hasClass("io.papermc.paper.configuration.Configuration") || hasClass("com.destroystokyo.paper.PaperConfig");
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
