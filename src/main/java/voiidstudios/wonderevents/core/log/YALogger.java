package voiidstudios.wonderevents.core.log;

public class YALogger {
    private final voiidstudios.tsunamilib.log.YALogger delegate;

    public YALogger(voiidstudios.tsunamilib.log.YALogger delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null.");
        }
        this.delegate = delegate;
    }

    public voiidstudios.tsunamilib.log.YALogger getDelegate() {
        return delegate;
    }

    public YALogger withName(String name) {
        return new YALogger(delegate.withName(name));
    }

    public void setDebug(boolean debug) {
        delegate.setDebug(debug);
    }

    public void debug(String message) {
        delegate.debug(message);
    }

    public void debug(String message, Throwable thrown) {
        delegate.debug(message, thrown);
    }

    public void debug(EpicLogLevel level, String message) {
        delegate.debug(level.toTsunami(), message);
    }

    public void console(String message) {
        delegate.console(message);
    }

    public void console(String message, Throwable thrown) {
        delegate.console(message, thrown);
    }

    public void info(String message) {
        delegate.info(message);
    }

    public void info(String message, Throwable thrown) {
        delegate.info(message, thrown);
    }

    public void passiveInfo(String message) {
        delegate.passiveInfo(message);
    }

    public void passiveInfo(String message, Throwable thrown) {
        delegate.passiveInfo(message, thrown);
    }

    public void process(String message) {
        delegate.process(message);
    }

    public void process(String message, Throwable thrown) {
        delegate.process(message, thrown);
    }

    public void passiveQuestion(String message) {
        delegate.passiveQuestion(message);
    }

    public void passiveQuestion(String message, Throwable thrown) {
        delegate.passiveQuestion(message, thrown);
    }

    public void success(String message) {
        delegate.success(message);
    }

    public void success(String message, Throwable thrown) {
        delegate.success(message, thrown);
    }

    public void failure(String message) {
        delegate.failure(message);
    }

    public void failure(String message, Throwable thrown) {
        delegate.failure(message, thrown);
    }

    public void warning(String message) {
        delegate.warning(message);
    }

    public void warning(String message, Throwable thrown) {
        delegate.warning(message, thrown);
    }

    public void passiveWarning(String message) {
        delegate.passiveWarning(message);
    }

    public void passiveWarning(String message, Throwable thrown) {
        delegate.passiveWarning(message, thrown);
    }

    public void severe(String message) {
        delegate.severe(message);
    }

    public void severe(String message, Throwable thrown) {
        delegate.severe(message, thrown);
    }

    public void passiveSevere(String message) {
        delegate.passiveSevere(message);
    }

    public void passiveSevere(String message, Throwable thrown) {
        delegate.passiveSevere(message, thrown);
    }
}