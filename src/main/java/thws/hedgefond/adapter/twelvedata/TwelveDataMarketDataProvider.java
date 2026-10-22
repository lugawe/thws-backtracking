package thws.hedgefond.adapter.twelvedata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.twelvedata.client.ws.PriceEvent;
import io.smallrye.mutiny.Multi;
import thws.hedgefond.adapter.twelvedata.TwelveDataClient.TimeSeries;
import thws.hedgefond.adapter.twelvedata.TwelveDataClient.TimeSeriesRequest;
import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.marketdata.Interval;
import thws.hedgefond.core.marketdata.MarketDataProvider;
import thws.hedgefond.core.marketdata.Quote;
import thws.hedgefond.core.money.CurrencyConverter;

@ApplicationScoped
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final TwelveDataClient client;

    @Inject
    public TwelveDataMarketDataProvider(TwelveDataClient client) {
        this.client = client;
    }

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        return client.getExchangeRate(from, to);
    }

    @Override
    public SequencedCollection<Candle> getHistory(
            String symbol, Interval interval, LocalDate start, LocalDate end, long limit, Currency currency) {

        symbol = Utils.normalizeSymbol(symbol);

        TimeSeriesRequest request = new TimeSeriesRequest(symbol, interval, start, end, limit);
        TimeSeries series =
                interval.isIntraday() ? client.getIntradayTimeSeries(request) : client.getTimeSeries(request);

        Function<BigDecimal, BigDecimal> c = converter(series.currency(), currency);
        return series.candles().stream().map(candle -> candle.convert(c)).toList();
    }

    @Override
    public Quote getQuote(String symbol, Currency currency) {
        Quote quote = fetchQuote(symbol);
        Function<BigDecimal, BigDecimal> c = converter(quote.currency(), currency);
        return quote.updateCurrency(currency, c);
    }

    @Override
    public Multi<Quote> streamQuotes(String symbol, Currency currency) {
        Quote quote = fetchQuote(symbol);
        Function<BigDecimal, BigDecimal> c = converter(quote.currency(), currency);
        return client.streamPrices(symbol)
                .onItem()
                .scan(() -> quote, TwelveDataMarketDataProvider::applyTick)
                .map(q -> q.updateCurrency(currency, c));
    }

    private Quote fetchQuote(String symbol) {
        symbol = Utils.normalizeSymbol(symbol);
        return client.getQuote(symbol);
    }

    private Function<BigDecimal, BigDecimal> converter(Currency from, Currency to) {
        return from.equals(to) ? Function.identity() : new CurrencyConverter(getExchangeRate(from, to));
    }

    private static Quote applyTick(Quote quote, PriceEvent tick) {
        long volume = Objects.requireNonNullElse(tick.getDayVolume(), quote.volume());
        return quote.updatePrice(BigDecimal.valueOf(tick.getPrice()), volume, tick.getTimestamp());
    }
}
