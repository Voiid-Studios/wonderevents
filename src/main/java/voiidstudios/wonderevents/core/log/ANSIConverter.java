package voiidstudios.wonderevents.core.log;

public final class ANSIConverter {
    private ANSIConverter() {}

    public static String convertToAnsi(String minecraftMessage) {
        return voiidstudios.tsunamilib.log.ANSIConverter.convertToAnsi(minecraftMessage);
    }
}