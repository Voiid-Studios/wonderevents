package voiidstudios.wonderevents.core.manifest;

import java.util.Locale;

public final class VersionConstraint {
    public enum Operator {
        EQ("=="),
        GTE(">="),
        LTE("<="),
        GT(">"),
        LT("<");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private final Operator operator;
    private final String version;

    public VersionConstraint(Operator operator, String version) {
        this.operator = operator;
        this.version = version;
    }

    public static VersionConstraint parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Version constraint cannot be empty");
        }

        String text = raw.trim();
        Operator operator = Operator.EQ;

        if (text.startsWith("==")) {
            operator = Operator.EQ;
            text = text.substring(2);
        } else if (text.startsWith(">=")) {
            operator = Operator.GTE;
            text = text.substring(2);
        } else if (text.startsWith("<=")) {
            operator = Operator.LTE;
            text = text.substring(2);
        } else if (text.startsWith(">")) {
            operator = Operator.GT;
            text = text.substring(1);
        } else if (text.startsWith("<")) {
            operator = Operator.LT;
            text = text.substring(1);
        }

        String version = text.trim();
        if (!version.matches("[0-9][0-9A-Za-z._+\\-]*")) {
            throw new IllegalArgumentException("Invalid version constraint '" + raw + "'. Use an operator (==, >=, <=, >, <) followed by a version, e.g. \">= 26.10.0\"");
        }

        return new VersionConstraint(operator, version);
    }

    public Operator getOperator() {
        return operator;
    }

    public String getVersion() {
        return version;
    }

    public boolean matches(String actualVersion) {
        if (actualVersion == null) {
            return false;
        }

        int cmp = VersionUtil.compare(VersionUtil.normalize(actualVersion), version);
        switch (operator) {
            case EQ:
                return cmp == 0;
            case GTE:
                return cmp >= 0;
            case LTE:
                return cmp <= 0;
            case GT:
                return cmp > 0;
            case LT:
                return cmp < 0;
            default:
                return false;
        }
    }

    public String toString() {
        return operator.getSymbol() + " " + version;
    }

    public String describe() {
        switch (operator) {
            case GTE: return "version " + version + " or higher";
            case LTE: return "version " + version + " or lower";
            case GT: return "a version higher than " + version;
            case LT: return "a version lower than " + version;
            default: return "version " + version + " exactly";
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VersionConstraint)) {
            return false;
        }
        VersionConstraint that = (VersionConstraint) other;
        return operator == that.operator && version.toLowerCase(Locale.ROOT).equals(that.version.toLowerCase(Locale.ROOT));
    }

    public int hashCode() {
        return operator.hashCode() * 31 + version.toLowerCase(Locale.ROOT).hashCode();
    }
}