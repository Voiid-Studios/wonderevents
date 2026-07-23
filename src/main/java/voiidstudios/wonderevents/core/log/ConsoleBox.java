package voiidstudios.wonderevents.core.log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ConsoleBox {
    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-ORX]");
    private static final Pattern AMPERSAND_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-ORX]");
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    private ConsoleBox() {}

    public static Builder builder() {
        return new Builder();
    }

    public static List<String> createBox(List<String> lines) {
        Builder builder = builder();
        lines.forEach(builder::line);
        return builder.build();
    }

    private static int visibleLength(String text) {
        return stripColors(text).length();
    }

    private static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String withoutAnsi = ANSI_PATTERN.matcher(text).replaceAll("");
        String withoutSectionColors = SECTION_COLOR_PATTERN.matcher(withoutAnsi).replaceAll("");
        return AMPERSAND_COLOR_PATTERN.matcher(withoutSectionColors).replaceAll("");
    }

    private static String repeat(char character, int amount) {
        if (amount <= 0) {
            return "";
        }

        return String.valueOf(character).repeat(amount);
    }

    private static String padRight(String text, int visibleLength, int targetLength) {
        return text + repeat(' ', targetLength - visibleLength);
    }

    private static String center(String text, int targetLength) {
        int visibleLength = visibleLength(text);
        int missing = Math.max(0, targetLength - visibleLength);
        int left = missing / 2;
        int right = missing - left;
        return repeat(' ', left) + text + repeat(' ', right);
    }

    public static final class Builder {
        private final List<String> lines = new ArrayList<>();
        private String title;
        private String footer;
        private String borderColor = "";
        private int minWidth;
        private int leftPadding = 1;
        private int rightPadding = 1;

        private Builder() {}

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        public Builder line(String line) {
            this.lines.add(line == null ? "" : line);
            return this;
        }

        public Builder blank() {
            this.lines.add("");
            return this;
        }

        public Builder borderColor(String color) {
            this.borderColor = color == null ? "" : color;
            return this;
        }

        public Builder minWidth(int width) {
            this.minWidth = Math.max(0, width);
            return this;
        }

        public Builder padding(int left, int right) {
            this.leftPadding = Math.max(0, left);
            this.rightPadding = Math.max(0, right);
            return this;
        }

        public List<String> build() {
            int contentWidth = calculateContentWidth();
            int textWidth = Math.max(0, contentWidth - leftPadding - rightPadding);
            List<String> result = new ArrayList<>();
            String border = border(contentWidth);

            result.add(border);

            if (title != null) {
                result.add(wrap(center(title, textWidth)));
                result.add(border);
            }

            for (String line : lines) {
                result.add(wrap(padRight(line, visibleLength(line), textWidth)));
            }

            if (footer != null) {
                result.add(border);
                result.add(wrap(center(footer, textWidth)));
            }

            result.add(border);
            return result;
        }

        private int calculateContentWidth() {
            int width = minWidth;

            if (title != null) {
                width = Math.max(width, visibleLength(title) + leftPadding + rightPadding);
            }

            if (footer != null) {
                width = Math.max(width, visibleLength(footer) + leftPadding + rightPadding);
            }

            for (String line : lines) {
                width = Math.max(width, visibleLength(line) + leftPadding + rightPadding);
            }

            return width;
        }

        private String border(int contentWidth) {
            return borderColor + "+" + repeat('-', contentWidth) + "+";
        }

        private String wrap(String content) {
            return borderColor + "|"
                    + repeat(' ', leftPadding)
                    + content
                    + repeat(' ', rightPadding)
                    + borderColor + "|";
        }
    }
}