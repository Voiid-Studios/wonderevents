package voiidstudios.wonderevents.core.log;

import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLoggerImpl implements EpicPlatformLogger {
    private final Logger logger;

    public JavaLoggerImpl(Logger logger) {
        this.logger = logger;
    }

    public void log(EpicLogLevel level, String message) {
        if (level == EpicLogLevel.WARNING) {
            logger.warning(message);
        } else if (level == EpicLogLevel.SEVERE) {
            logger.severe(message);
        } else {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    public void log(EpicLogLevel level, String message, Throwable throwable) {
        if (level == EpicLogLevel.WARNING) {
            logger.log(Level.WARNING, message, throwable);
        } else if (level == EpicLogLevel.SEVERE) {
            logger.log(Level.SEVERE, message, throwable);
        } else {
            Bukkit.getConsoleSender().sendMessage(message);
            throwable.printStackTrace();
        }
    }
}