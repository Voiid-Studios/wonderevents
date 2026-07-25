package voiidstudios.wonderevents.update;

public final class UpdateCheckerResult {
    private final String latestVersion;
    private final boolean error;
    private final String errorMessage;

    private UpdateCheckerResult(String latestVersion, boolean error, String errorMessage) {
        this.latestVersion = latestVersion;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public String getLatestVersion() { return latestVersion; }
    public boolean isError() { return error; }
    public String getErrorMessage() { return errorMessage; }

    public static UpdateCheckerResult noErrors(String latestVersion) {
        return new UpdateCheckerResult(latestVersion, false, null);
    }

    public static UpdateCheckerResult error(String message) {
        return new UpdateCheckerResult(null, true, message);
    }
}
