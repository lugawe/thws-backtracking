package thws.hedgefond.api.mcp.marketdata;

import java.time.LocalDate;
import java.util.Currency;
import java.util.SequencedCollection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.marketdata.Interval;
import thws.hedgefond.core.marketdata.MarketDataException;
import thws.hedgefond.core.marketdata.MarketDataProvider;
import thws.hedgefond.core.marketdata.Quote;

@Singleton
public class StockTools {

    private final MarketDataProvider provider;

    @Inject
    public StockTools(MarketDataProvider provider) {
        this.provider = provider;
    }

    @Tool(
            name = "get_stock_quote",
            description = "Returns the current quote of a stock.",
            annotations =
                    @Tool.Annotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = true))
    public Quote getStockQuote(
            @ToolArg(description = "Ticker symbol of the stock (e.g. AAPL)") String symbol,
            @ToolArg(description = "Currency code of the prices (ISO 4217, e.g. EUR)", defaultValue = "EUR")
                    String currency) {

        Currency c = toCurrency(currency);
        try {
            return provider.getQuote(symbol, c);
        } catch (MarketDataException e) {
            throw new ToolCallException(e.getMessage(), e);
        }
    }

    @Tool(
            name = "get_stock_candles",
            description = "Returns the historical OHLC candles of a stock.",
            annotations =
                    @Tool.Annotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = true))
    public SequencedCollection<Candle> getStockCandles(
            @ToolArg(description = "Ticker symbol of the stock (e.g. AAPL)") String symbol,
            @ToolArg(description = "Length of one candle", defaultValue = "DAY_1") Interval interval,
            @ToolArg(description = "First day to include (ISO date, e.g. 2026-01-31)") LocalDate start,
            @ToolArg(description = "Last day to include (ISO date, e.g. 2026-01-31)") LocalDate end,
            @ToolArg(description = "Maximum number of candles", defaultValue = "30") long limit,
            @ToolArg(description = "Currency code of the prices (ISO 4217, e.g. EUR)", defaultValue = "EUR")
                    String currency) {

        Currency c = toCurrency(currency);
        try {
            return provider.getHistory(symbol, interval, start, end, limit, c);
        } catch (MarketDataException e) {
            throw new ToolCallException(e.getMessage(), e);
        }
    }

    private static Currency toCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("Unknown currency code: " + code);
        }
    }
}
