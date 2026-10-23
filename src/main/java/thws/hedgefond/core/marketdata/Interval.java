package thws.hedgefond.core.marketdata;

public enum Interval {
    MINUTES_1,
    MINUTES_5,
    HOURS_1,
    DAY_1,
    WEEK_1;

    public boolean isIntraday() {
        return switch (this) {
            case MINUTES_1, MINUTES_5, HOURS_1 -> true;
            case DAY_1, WEEK_1 -> false;
        };
    }
}
