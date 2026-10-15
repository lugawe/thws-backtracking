package thws.hedgefond.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Function;

/**
 * Snapshot of the current state of a stock: "what is the price right now and how did it move today?".
 *
 * @param symbol ticker symbol, e.g. {@code AAPL}
 * @param name company name
 * @param exchange exchange the quote comes from, e.g. {@code NASDAQ}
 * @param currency currency of all prices, e.g. {@code USD}
 * @param price last traded price; the closing price if the market is closed
 * @param open first price of the current trading day
 * @param high highest price of the current trading day
 * @param low lowest price of the current trading day
 * @param previousClose closing price of the previous trading day
 * @param change absolute change of {@code price} compared to {@code previousClose}
 * @param percentChange change in percent compared to {@code previousClose}, e.g. {@code 0.5} for +0.5%
 * @param volume number of shares traded on the current trading day
 * @param marketOpen whether the exchange is currently open
 * @param timestamp time of the last price update
 */
public record Quote(
        String symbol,
        String name,
        String exchange,
        String currency,
        BigDecimal price,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal previousClose,
        BigDecimal change,
        BigDecimal percentChange,
        long volume,
        boolean marketOpen,
        long timestamp) {

    public Quote updatePrice(BigDecimal price, long timestamp) {

        BigDecimal change = price.subtract(previousClose);
        BigDecimal percentChange =
                change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 6, RoundingMode.HALF_UP);

        return new Quote(
                symbol,
                name,
                exchange,
                currency,
                price,
                open,
                high.max(price),
                low.min(price),
                previousClose,
                change,
                percentChange,
                volume,
                marketOpen,
                timestamp);
    }

    public Quote updateCurrency(String newCurrency, Function<BigDecimal, BigDecimal> converter) {
        return new Quote(
                symbol,
                name,
                exchange,
                Objects.requireNonNull(newCurrency),
                converter.apply(price),
                converter.apply(open),
                converter.apply(high),
                converter.apply(low),
                converter.apply(previousClose),
                converter.apply(change),
                percentChange,
                volume,
                marketOpen,
                timestamp);
    }
}
