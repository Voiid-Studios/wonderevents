package voiidstudios.wonderevents.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.bukkit.plugin.Plugin;

public final class DownloadSource {
    public static final String UNKNOWN = "Unknown";

    private DownloadSource() {}

    public static String detect(Plugin plugin) {
        try (InputStream inputStream = plugin.getClass().getResourceAsStream("/download-source.txt")) {
            if (inputStream == null) return UNKNOWN;

            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                String line = bufferedReader.readLine();
                return line == null ? UNKNOWN : line;
            }
        } catch (IOException e) {
            return UNKNOWN;
        }
    }
}
