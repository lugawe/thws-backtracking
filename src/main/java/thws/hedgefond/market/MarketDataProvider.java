package thws.hedgefond.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.SequencedCollection;

import io.smallrye.mutiny.Multi;

public interface MarketDataProvider {

    enum Interval {
        MINUTES_1("1min"),
        MINUTES_5("5min"),
        HOURS_1("1h"),
        DAY_1("1day"),
        WEEK_1("1week");

        private final String interval;

        Interval(String interval) {
            this.interval = interval;
        }

        public String getInterval() {
            return interval;
        }

        public static Interval fromString(String value) {
            for (Interval i : values()) {
                if (i.interval.equals(value)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Unknown interval: " + value);
        }
    }

    BigDecimal getExchangeRate(Currency from, Currency to);

    Quote getQuote(String symbol, Currency currency);

    Multi<Quote> streamQuotes(String symbol, Currency currency);

    SequencedCollection<Candle> getHistory(
            String symbol, Interval interval, LocalDate start, LocalDate end, long limit, Currency currency);
}
