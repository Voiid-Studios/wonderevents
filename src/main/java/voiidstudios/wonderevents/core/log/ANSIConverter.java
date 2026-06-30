package voiidstudios.wonderevents.core.log;

import java.util.HashMap;
import java.util.Map;

public final class ANSIConverter {
    private static final Map<EpicChatColor, String> replacements = new HashMap<EpicChatColor, String>();

    static {
        replacements.put(EpicChatColor.BLACK, "[30;22m");
        replacements.put(EpicChatColor.DARK_BLUE, "[34;22m");
        replacements.put(EpicChatColor.DARK_GREEN, "[32;22m");
        replacements.put(EpicChatColor.DARK_AQUA, "[36;22m");
        replacements.put(EpicChatColor.DARK_RED, "[31;22m");
        replacements.put(EpicChatColor.DARK_PURPLE, "[35;22m");
        replacements.put(EpicChatColor.GOLD, "[33;22m");
        replacements.put(EpicChatColor.GRAY, "[37;22m");
        replacements.put(EpicChatColor.DARK_GRAY, "[30;1m");
        replacements.put(EpicChatColor.BLUE, "[34;1m");
        replacements.put(EpicChatColor.GREEN, "[32;1m");
        replacements.put(EpicChatColor.AQUA, "[36;1m");
        replacements.put(EpicChatColor.RED, "[31;1m");
        replacements.put(EpicChatColor.LIGHT_PURPLE, "[35;1m");
        replacements.put(EpicChatColor.YELLOW, "[33;1m");
        replacements.put(EpicChatColor.WHITE, "[37;1m");
        replacements.put(EpicChatColor.MAGIC, "[5m");
        replacements.put(EpicChatColor.BOLD, "[21m");
        replacements.put(EpicChatColor.STRIKETHROUGH, "[9m");
        replacements.put(EpicChatColor.UNDERLINE, "[4m");
        replacements.put(EpicChatColor.ITALIC, "[3m");
        replacements.put(EpicChatColor.RESET, "[0;39m");
    }

    private ANSIConverter() {}

    public static String convertToAnsi(String minecraftMessage) {
        String result = minecraftMessage;
        for (EpicChatColor color : EpicChatColor.values()) {
            result = result.replaceAll("(?i)" + color.toString(), replacements.containsKey(color) ? replacements.get(color) : "");
        }
        return result;
    }
}