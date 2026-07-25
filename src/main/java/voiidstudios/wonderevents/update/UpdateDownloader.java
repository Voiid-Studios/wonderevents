package voiidstudios.wonderevents.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Bukkit;

import voiidstudios.wonderevents.core.log.YALogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class UpdateDownloader {
    private static final String USER_AGENT = "WonderEvents-Updater";

    private final YALogger logger;
    private final UpdateChecker updateChecker;

    public UpdateDownloader(YALogger logger, UpdateChecker updateChecker) {
        this.logger = logger;
        this.updateChecker = updateChecker;
    }

    public boolean downloadUpdate() {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(UpdateChecker.apiUrl()).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            if (connection.getResponseCode() != 200) {
                logger.passiveWarning("[Update] GitHub API responded with code " + connection.getResponseCode());
                return false;
            }

            JsonObject release = JsonParser.parseReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            ).getAsJsonObject();

            if (release.has("prerelease") && release.get("prerelease").getAsBoolean()) {
                return false;
            }

            String downloadUrl = null;
            String assetName = null;

            for (JsonElement element : release.getAsJsonArray("assets")) {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.toLowerCase().contains("wonderevents") && name.endsWith(".jar")) {
                    downloadUrl = asset.get("browser_download_url").getAsString();
                    assetName = name;
                    break;
                }
            }

            if (downloadUrl == null || assetName == null) {
                logger.passiveWarning("[Update] Could not find a WonderEvents jar in the latest release assets.");
                return false;
            }

            long start = System.currentTimeMillis();

            Path updateFile = Bukkit.getUpdateFolderFile().toPath().resolve(assetName);
            Files.createDirectories(updateFile.getParent());

            HttpURLConnection downloadConnection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            downloadConnection.setRequestProperty("User-Agent", USER_AGENT);
            downloadConnection.setConnectTimeout(10_000);
            downloadConnection.setReadTimeout(10_000);

            try (InputStream in = downloadConnection.getInputStream()) {
                Files.copy(in, updateFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long elapsed = System.currentTimeMillis() - start;
            logger.success("[Update] Downloaded update in " + elapsed + "ms!");
            logger.passiveWarning("[Update] WonderEvents will update from §6" + updateChecker.getCurrentVersion() + "§r to §9" + updateChecker.getLatestVersion() + "§r on the next server restart!");
            return true;
        } catch (Exception ex) {
            logger.failure("[Update] Failed to download update: " + ex.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
