package thws.hedgefond.core.marketdata;

public enum Interval {
    MINUTES_1("1min"),
    MINUTES_5("5min"),
    HOURS_1("1h"),
    DAY_1("1day"),
    WEEK_1("1week");

    private final String label;

    Interval(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Interval fromString(String value) {
        for (Interval i : values()) {
            if (i.label.equals(value)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown interval: " + value);
    }
}
