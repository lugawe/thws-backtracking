package thws.hedgefond.market;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Summary of the price movement of a stock over one time period (e.g. one day or five minutes),
 * also known as OHLC (open, high, low, close).
 *
 * @param dateTime start of the period, in the exchange's timezone (not UTC); midnight for daily or longer intervals
 * @param open first traded price in the period
 * @param high highest traded price in the period
 * @param low lowest traded price in the period
 * @param close last traded price in the period
 * @param volume number of shares traded in the period
 */
public record Candle(
        ZonedDateTime dateTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {}
