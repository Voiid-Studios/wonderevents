package voiidstudios.wonderevents.core.manifest;

/**
 * Tiny version helper that compares dotted numeric versions.
 *
 * <p>It ignores everything after a {@code +} suffix so a version such as
 * {@code 26.06.3+indevABCD} is treated as {@code 26.06.3}.
 */
public final class VersionUtil {

    private VersionUtil() {
    }

    public static String normalize(String version) {
        if (version == null) {
            return "0.0.0";
        }
        String cleaned = version.trim();
        int plusIndex = cleaned.indexOf('+');
        if (plusIndex >= 0) {
            cleaned = cleaned.substring(0, plusIndex);
        }
        return cleaned.isBlank() ? "0.0.0" : cleaned;
    }

    public static boolean isAtLeast(String currentVersion, String minimumVersion) {
        return compare(normalize(currentVersion), normalize(minimumVersion)) >= 0;
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
        if (raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
