package voiidstudios.wonderevents.core.log;

public interface EpicPlatformLogger {
    void log(EpicLogLevel level, String message);
    void log(EpicLogLevel level, String message, Throwable throwable);
}