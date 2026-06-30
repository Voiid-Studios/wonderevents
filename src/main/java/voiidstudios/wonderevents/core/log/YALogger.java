package voiidstudios.wonderevents.core.log;

public class YALogger {
    private final EpicPlatformLogger logger;
    private final boolean color;
    private boolean debug;

    public YALogger(EpicPlatformLogger logger, boolean color) {
        this.logger = logger;
        this.color  = color;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }


    public void debug(String message) {
        if (!debug) return;
        log(EpicLogLevel.INFO, message);
    }

    public void debug(String message, Throwable thrown) {
        if (!debug) return;
        log(EpicLogLevel.WARNING, message, thrown);
    }

    public void debug(EpicLogLevel level, String message) {
        if (!debug) return;
        log(level, message);
    }


    public void console(String message) {
        log(EpicLogLevel.CONSOLE, message);
    }

    public void console(String message, Throwable thrown) {
        log(EpicLogLevel.CONSOLE, message, thrown);
    }

    public void info(String message) {
        log(EpicLogLevel.INFO, message);
    }

    public void info(String message, Throwable thrown) {
        log(EpicLogLevel.INFO, message, thrown);
    }

    public void passiveInfo(String message) {
        log(EpicLogLevel.PASSIVE_INFO, message);
    }

    public void passiveInfo(String message, Throwable thrown) {
        log(EpicLogLevel.PASSIVE_INFO, message, thrown);
    }

    public void process(String message) {
        log(EpicLogLevel.PROCESS, message);
    }

    public void process(String message, Throwable thrown) {
        log(EpicLogLevel.PROCESS, message, thrown);
    }

    public void passiveQuestion(String message) {
        log(EpicLogLevel.PASSIVE_QUESTION, message);
    }

    public void passiveQuestion(String message, Throwable thrown) {
        log(EpicLogLevel.PASSIVE_QUESTION, message, thrown);
    }
    
    public void success(String message) {
        log(EpicLogLevel.SUCCESS, message);
    }

    public void success(String message, Throwable thrown) {
        log(EpicLogLevel.SUCCESS, message, thrown);
    }

    public void failure(String message) {
        log(EpicLogLevel.FAILURE, message);
    }

    public void failure(String message, Throwable thrown) {
        log(EpicLogLevel.FAILURE, message, thrown);
    }

    public void warning(String message) {
        log(EpicLogLevel.WARNING, message);
    }

    public void warning(String message, Throwable thrown) {
        log(EpicLogLevel.WARNING, message, thrown);
    }

    public void passiveWarning(String message) {
        log(EpicLogLevel.PASSIVE_WARNING, message);
    }

    public void passiveWarning(String message, Throwable thrown) {
        log(EpicLogLevel.PASSIVE_WARNING, message, thrown);
    }

    public void severe(String message) {
        log(EpicLogLevel.SEVERE, message);
    }

    public void severe(String message, Throwable thrown) {
        log(EpicLogLevel.SEVERE, message, thrown);
    }

    public void passiveSevere(String message) {
        log(EpicLogLevel.PASSIVE_SEVERE, message);
    }

    public void passiveSevere(String message, Throwable thrown) {
        log(EpicLogLevel.PASSIVE_SEVERE, message, thrown);
    }


    private void log(EpicLogLevel level, String message) {
        logger.log(level, formatMessage(level, message));
    }

    private void log(EpicLogLevel level, String message, Throwable thrown) {
        logger.log(level, formatMessage(level, message), thrown);
    }

    private String formatMessage(EpicLogLevel level, String message) {
        if (color) {
            String prefix = getPrefix(level);
            String levelColor;

            if (level == EpicLogLevel.SUCCESS) {
                levelColor = "[§a✓§r] ";
            } else if (level == EpicLogLevel.FAILURE) {
                levelColor = "[§c×§r] ";
            } else if (level == EpicLogLevel.PASSIVE_INFO) {
                levelColor = "[§9!§r] ";
            } else if (level == EpicLogLevel.PROCESS) {
                levelColor = "[-] ";
            } else if (level == EpicLogLevel.PASSIVE_QUESTION) {
                levelColor = "[§6?§r] ";
            } else if (level == EpicLogLevel.WARNING) {
                levelColor = "§e";
            } else if (level == EpicLogLevel.PASSIVE_WARNING) {
                levelColor = "[§e!§r] ";
            } else if (level == EpicLogLevel.SEVERE) {
                levelColor = "§c";
            } else if (level == EpicLogLevel.PASSIVE_SEVERE) {
                levelColor = "[§c!§r] ";
            } else {
                levelColor = "";
            }

            if (level == EpicLogLevel.CONSOLE) {
                message = levelColor + message + "§r";
            } else {
                message = prefix + levelColor + message + "§r";
            }
        }
        return ANSIConverter.convertToAnsi(message);
    }

    private String getPrefix(EpicLogLevel level) {
        if (level == EpicLogLevel.WARNING) {
            return "§e[WonderEvents] ";
        }
        if (level == EpicLogLevel.SEVERE) {
            return "§c[WonderEvents] ";
        }
        return "§d[§bWonderEvents§d] §r";
    }
}
