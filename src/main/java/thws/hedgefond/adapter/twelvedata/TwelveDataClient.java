package thws.hedgefond.adapter.twelvedata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
import com.twelvedata.client.model.TimeSeriesItem;
import com.twelvedata.client.ws.PriceEvent;
import com.twelvedata.client.ws.TwelvedataWebSocketClient;
import com.twelvedata.client.ws.TwelvedataWebSocketOptions;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.marketdata.Interval;
import thws.hedgefond.core.marketdata.Quote;

@Singleton
public class TwelveDataClient {

    private final MarketDataApi api;
    private final CurrenciesApi currencies;
    private final TwelveDataPriceStream priceStream;

    @Inject
    public TwelveDataClient(@ConfigProperty(name = "twelvedata.api-key") String apiKey) {
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

    @CacheResult(cacheName = "exchange-rates")
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        GetExchangeRate200Response r = Utils.call(() -> currencies.getExchangeRate(
                APIGetExchangeRateRequest.newBuilder().symbol(from + "/" + to).build()));
        return BigDecimal.valueOf(r.getRate());
    }

    @CacheResult(cacheName = "candles")
    public TimeSeries getTimeSeries(TimeSeriesRequest request) {
        return fetchTimeSeries(request);
    }

    @CacheResult(cacheName = "candles-intraday")
    public TimeSeries getIntradayTimeSeries(TimeSeriesRequest request) {
        return fetchTimeSeries(request);
    }

    @CacheResult(cacheName = "quotes")
    public Quote getQuote(String symbol) {
        GetQuote200Response r = Utils.call(() ->
                api.getQuote(APIGetQuoteRequest.newBuilder().symbol(symbol).build()));
        return toQuote(r);
    }

    public Multi<PriceEvent> streamPrices(String symbol) {
        return priceStream.prices(symbol);
    }

    private TimeSeries fetchTimeSeries(TimeSeriesRequest request) {
        GetTimeSeries200Response r = Utils.call(() -> api.getTimeSeries(APIGetTimeSeriesRequest.newBuilder()
                .symbol(request.symbol())
                .interval(Utils.toIntervalEnum(request.interval()))
                .startDate(Objects.toString(request.start(), null))
                .endDate(request.end() != null ? request.end().plusDays(1).toString() : null) // end_date is exclusive
                .outputsize(request.limit())
                .timezone("UTC")
                .build()));

        List<Candle> candles =
                r.getValues().stream().map(TwelveDataClient::toCandle).toList();
        Currency currency = Currency.getInstance(r.getMeta().getCurrency());

        return new TimeSeries(candles, currency);
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

    private static Quote toQuote(GetQuote200Response r) {
        return new Quote(
                Objects.requireNonNullElse(r.getLastQuoteAt(), r.getTimestamp()),
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
                r.getIsMarketOpen());
    }

    public record TimeSeriesRequest(String symbol, Interval interval, LocalDate start, LocalDate end, long limit) {}

    public record TimeSeries(SequencedCollection<Candle> candles, Currency currency) {}
}
