package thws.hedgefond.market;

import java.math.BigDecimal;
import java.time.Instant;

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
        Instant timestamp) {}
