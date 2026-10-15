package thws.hedgefond.adapter.twelvedata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.twelvedata.client.ApiClient;
import com.twelvedata.client.Twelvedata;
import com.twelvedata.client.api.CurrenciesApi;
import com.twelvedata.client.api.CurrenciesApi.APIGetExchangeRateRequest;
import com.twelvedata.client.api.MarketDataApi;
import com.twelvedata.client.api.MarketDataApi.APIGetQuoteRequest;
import com.twelvedata.client.api.MarketDataApi.APIGetTimeSeriesRequest;
import com.twelvedata.client.model.GetExchangeRate200Response;
import com.twelvedata.client.model.GetQuote200Response;
import com.twelvedata.client.model.GetTimeSeries200Response;
import com.twelvedata.client.model.IntervalEnum;
import com.twelvedata.client.model.TimeSeriesItem;
import com.twelvedata.client.ws.PriceEvent;
import com.twelvedata.client.ws.TwelvedataWebSocketClient;
import com.twelvedata.client.ws.TwelvedataWebSocketOptions;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.marketdata.Interval;
import thws.hedgefond.core.marketdata.MarketDataProvider;
import thws.hedgefond.core.marketdata.Quote;
import thws.hedgefond.core.money.CurrencyConverter;

@ApplicationScoped
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final MarketDataApi api;
    private final CurrenciesApi currencies;
    private final TwelveDataPriceStream priceStream;

    @Inject
    public TwelveDataMarketDataProvider(@ConfigProperty(name = "twelvedata.api-key") String apiKey) {
        ApiClient client = Twelvedata.newApiClient(apiKey);
        this.api = new MarketDataApi(client);
        this.currencies = new CurrenciesApi(client);
        this.priceStream = new TwelveDataPriceStream(new TwelvedataWebSocketClient(
                TwelvedataWebSocketOptions.builder().apiKey(apiKey).build()));
    }

    @PreDestroy
    public void shutdown() {
        priceStream.disconnect();
    }

    @Override
    @CacheResult(cacheName = "exchange-rates")
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        GetExchangeRate200Response r = Utils.call(() -> currencies.getExchangeRate(
                APIGetExchangeRateRequest.newBuilder().symbol(from + "/" + to).build()));
        return BigDecimal.valueOf(r.getRate());
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
        return priceStream
                .prices(symbol)
                .onItem()
                .scan(() -> quote, TwelveDataMarketDataProvider::applyTick)
                .map(q -> q.updateCurrency(currency, c));
    }

    @Override
    public SequencedCollection<Candle> getHistory(
            String symbol, Interval interval, LocalDate start, LocalDate end, long limit, Currency currency) {

        GetTimeSeries200Response r = Utils.call(() -> api.getTimeSeries(APIGetTimeSeriesRequest.newBuilder()
                .symbol(symbol)
                .interval(toIntervalEnum(interval))
                .startDate(Objects.toString(start, null))
                .endDate(end != null ? end.plusDays(1).toString() : null) // end_date is exclusive
                .outputsize(limit)
                .timezone("UTC")
                .build()));

        Function<BigDecimal, BigDecimal> c =
                converter(Currency.getInstance(r.getMeta().getCurrency()), currency);

        return r.getValues().stream().map(v -> toCandle(v).convert(c)).toList();
    }

    private Quote fetchQuote(String symbol) {
        GetQuote200Response r = Utils.call(() ->
                api.getQuote(APIGetQuoteRequest.newBuilder().symbol(symbol).build()));
        return toQuote(r);
    }

    private Function<BigDecimal, BigDecimal> converter(Currency from, Currency to) {
        return from.equals(to) ? Function.identity() : new CurrencyConverter(getExchangeRate(from, to));
    }

    private static Quote toQuote(GetQuote200Response r) {
        return new Quote(
                r.getSymbol(),
                r.getName(),
                r.getExchange(),
                Currency.getInstance(r.getCurrency()),
                Utils.decimal(r.getClose()),
                Utils.decimal(r.getOpen()),
                Utils.decimal(r.getHigh()),
                Utils.decimal(r.getLow()),
                Utils.decimal(r.getPreviousClose()),
                Utils.decimal(r.getChange()),
                Utils.decimal(r.getPercentChange()),
                Utils.number(r.getVolume()),
                r.getIsMarketOpen(),
                Objects.requireNonNullElse(r.getLastQuoteAt(), r.getTimestamp()));
    }

    private static Candle toCandle(TimeSeriesItem v) {
        return new Candle(
                Utils.timestamp(v.getDatetime()),
                Utils.decimal(v.getOpen()),
                Utils.decimal(v.getHigh()),
                Utils.decimal(v.getLow()),
                Utils.decimal(v.getClose()),
                Utils.number(v.getVolume()));
    }

    private static Quote applyTick(Quote quote, PriceEvent tick) {
        long volume = Objects.requireNonNullElse(tick.getDayVolume(), quote.volume());
        return quote.updatePrice(BigDecimal.valueOf(tick.getPrice()), volume, tick.getTimestamp());
    }

    private static IntervalEnum toIntervalEnum(Interval interval) {
        return switch (interval) {
            case MINUTES_1 -> IntervalEnum._1MIN;
            case MINUTES_5 -> IntervalEnum._5MIN;
            case HOURS_1 -> IntervalEnum._1H;
            case DAY_1 -> IntervalEnum._1DAY;
            case WEEK_1 -> IntervalEnum._1WEEK;
        };
    }
}
