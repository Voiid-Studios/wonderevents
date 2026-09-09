package voiidstudios.wonderevents.core.manifest;

public final class VersionUtil {
    private VersionUtil() {}

    public static String normalize(String version) {
        if (version == null) {
            return "0.0.0";
        }
        String cleaned = version.trim();
        int plusIndex = cleaned.indexOf('+');
        if (plusIndex >= 0) {
            cleaned = cleaned.substring(0, plusIndex);
        }
        return cleaned.trim().isEmpty() ? "0.0.0" : cleaned;
    }

    public static boolean isAtLeast(String currentVersion, String minimumVersion) {
        return compare(normalize(currentVersion), normalize(minimumVersion)) >= 0;
    }

    public static boolean isAtMost(String currentVersion, String maximumVersion) {
        return compare(normalize(currentVersion), normalize(maximumVersion)) <= 0;
    }

    public static int compare(String left, String right) {
        String[] a = normalize(left).split("\\.");
        String[] b = normalize(right).split("\\.");
        int max = Math.max(a.length, b.length);

        for (int i = 0; i < max; i++) {
            int av = parsePart(a, i);
            int bv = parsePart(b, i);
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String raw = parts[index].replaceAll("[^0-9].*$", "");
        if (raw.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
