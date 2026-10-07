package voiidstudios.wonderevents.core.log;

import java.util.List;

public final class ConsoleBox {
    private ConsoleBox() {}

    public static Builder builder() {
        return new Builder(voiidstudios.tsunamilib.log.ConsoleBox.builder());
    }

    public static List<String> createBox(List<String> lines) {
        return voiidstudios.tsunamilib.log.ConsoleBox.createBox(lines);
    }

    public static final class Builder {
        private final voiidstudios.tsunamilib.log.ConsoleBox.Builder delegate;

        private Builder(voiidstudios.tsunamilib.log.ConsoleBox.Builder delegate) {
            this.delegate = delegate;
        }

        public Builder title(String title) {
            delegate.title(title);
            return this;
        }

        public Builder footer(String footer) {
            delegate.footer(footer);
            return this;
        }

        public Builder line(String line) {
            delegate.line(line);
            return this;
        }

        public Builder blank() {
            delegate.blank();
            return this;
        }

        public Builder borderColor(String color) {
            delegate.borderColor(color);
            return this;
        }

        public Builder minWidth(int width) {
            delegate.minWidth(width);
            return this;
        }

        public Builder padding(int left, int right) {
            delegate.padding(left, right);
            return this;
        }

        public List<String> build() {
            return delegate.build();
        }
    }
}