package thws.hedgefond.core.marketdata;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

/**
 * Price history of a stock as a sequence of candles of the same length.
 *
 * @param symbol ticker symbol, e.g. {@code AAPL}
 * @param interval length of one candle
 * @param currency currency of all prices, e.g. {@code USD}
 * @param candles the candles, newest first
 */
public record CandleSeries(String symbol, Interval interval, Currency currency, SequencedCollection<Candle> candles) {

    /**
     * Combines all candles into one candle spanning the whole series.
     *
     * @return the combined candle, or {@code null} if the series is empty
     */
    public Candle getAggregate() {

        if (candles.isEmpty()) {
            return null;
        }

        Candle oldest = candles.getLast();
        Candle newest = candles.getFirst();

        return new Candle(
                oldest.timestamp(),
                oldest.open(),
                candles.stream().map(Candle::high).reduce(BigDecimal::max).orElseThrow(),
                candles.stream().map(Candle::low).reduce(BigDecimal::min).orElseThrow(),
                newest.close(),
                candles.stream().mapToLong(Candle::volume).sum());
    }

    public CandleSeries updateCurrency(Currency newCurrency, Function<BigDecimal, BigDecimal> converter) {
        return new CandleSeries(
                symbol,
                interval,
                Objects.requireNonNull(newCurrency),
                candles.stream().map(candle -> candle.convert(converter)).toList());
    }
}
