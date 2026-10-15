package thws.hedgefond.market;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Summary of the price movement of a stock over one time period (e.g. one day or five minutes),
 * also known as OHLC (open, high, low, close).
 *
 * @param timestamp start of the period
 * @param open first traded price in the period
 * @param high highest traded price in the period
 * @param low lowest traded price in the period
 * @param close last traded price in the period
 * @param volume number of shares traded in the period
 */
public record Candle(long timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {

    public Candle convert(Function<BigDecimal, BigDecimal> converter) {
        return new Candle(
                timestamp,
                converter.apply(open),
                converter.apply(high),
                converter.apply(low),
                converter.apply(close),
                volume);
    }
}
