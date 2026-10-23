package thws.hedgefond.api.rest.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.marketdata.Interval;
import thws.hedgefond.core.marketdata.MarketDataProvider;
import thws.hedgefond.core.strategy.IndicatorDefinition;
import thws.hedgefond.core.strategy.StrategyException;
import thws.hedgefond.core.strategy.indicator.BollingerBands;
import thws.hedgefond.core.strategy.indicator.IndicatorFactory;

@Path("/indicators")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IndicatorResource {

    private final MarketDataProvider provider;

    @Inject
    public IndicatorResource(MarketDataProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("/evaluate")
    public IndicatorResponse evaluate(IndicatorRequest request) {
        try {
            if (request.indicators() == null || request.indicators().isEmpty()) {
                throw new BadRequestException("At least one indicator is required");
            }

            List<Candle> candles = new ArrayList<>(provider.getHistory(
                            request.symbol(),
                            request.interval(),
                            request.start(),
                            request.end(),
                            request.limit(),
                            request.currency())
                    .candles());
            candles = candles.reversed();

            Map<String, List<Object>> values = new LinkedHashMap<>();
            for (IndicatorSpec spec : request.indicators()) {
                if (spec == null || spec.name() == null || spec.name().isBlank()
                        || spec.type() == null || spec.arguments() == null) {
                    throw new BadRequestException("Every indicator needs a name, type, and arguments");
                }
                var definition = new IndicatorDefinition(spec.type().toLowerCase(), spec.arguments());
                var indicator = IndicatorFactory.create(definition);
                List<Object> series = new ArrayList<>();
                for (int index = 0; index < candles.size(); index++) {
                    Object value = indicator.calculate(candles, index);
                    series.add(value instanceof BollingerBands bands
                            ? new BollingerValue(bands.middle(), bands.upper(), bands.lower())
                            : value);
                }
                values.put(spec.name(), series);
            }

            List<CandleValues> result = new ArrayList<>();
            for (int index = 0; index < candles.size(); index++) {
                Map<String, Object> indicators = new LinkedHashMap<>();
                for (var entry : values.entrySet()) indicators.put(entry.getKey(), entry.getValue().get(index));
                Candle candle = candles.get(index);
                result.add(new CandleValues(candle.timestamp(), candle.close(), indicators));
            }
            return new IndicatorResponse(request.symbol(), request.interval(), request.currency(), result);
        } catch (StrategyException | IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record IndicatorRequest(
            String symbol,
            Interval interval,
            LocalDate start,
            LocalDate end,
            long limit,
            Currency currency,
            List<IndicatorSpec> indicators) {

        public IndicatorRequest {
            if (interval == null) interval = Interval.DAY_1;
            if (limit <= 0) limit = 500;
            if (currency == null) currency = Currency.getInstance("EUR");
        }
    }

    /** Arguments use the same extensible form as the strategy DSL, e.g. ["close", "14"]. */
    public record IndicatorSpec(String name, String type, List<String> arguments) {}

    public record IndicatorResponse(
            String symbol, Interval interval, Currency currency, List<CandleValues> candles) {}

    public record CandleValues(long timestamp, BigDecimal close, Map<String, Object> indicators) {}

    public record BollingerValue(BigDecimal middle, BigDecimal upper, BigDecimal lower) {}
}
