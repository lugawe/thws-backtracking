package thws.hedgefond.api.strategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

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
import thws.hedgefond.core.strategy.StrategyEngine;
import thws.hedgefond.core.strategy.StrategyException;
import thws.hedgefond.core.strategy.StrategySignal;
import thws.hedgefond.core.strategy.dsl.StrategyParser;

@Path("/strategies")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StrategyResource {

    private final MarketDataProvider provider;
    private final StrategyParser parser = new StrategyParser();
    private final StrategyEngine engine = new StrategyEngine();

    @Inject
    public StrategyResource(MarketDataProvider provider) {
        this.provider = provider;
    }

    @POST
    @Path("/evaluate")
    public EvaluationResponse evaluate(EvaluationRequest request) {
        try {
            var strategy = parser.parse(request.strategy());
            List<Candle> candles = new ArrayList<>(provider.getHistory(
                    request.symbol(),
                    request.interval(),
                    request.start(),
                    request.end(),
                    request.limit(),
                    request.currency()));
            candles = candles.reversed();
            return new EvaluationResponse(strategy.name(), request.symbol(), engine.evaluate(strategy, candles));
        } catch (StrategyException | IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record EvaluationRequest(
            String symbol,
            Interval interval,
            LocalDate start,
            LocalDate end,
            long limit,
            Currency currency,
            String strategy) {

        public EvaluationRequest {
            if (interval == null) interval = Interval.DAY_1;
            if (limit <= 0) limit = 500;
            if (currency == null) currency = Currency.getInstance("EUR");
        }
    }

    public record EvaluationResponse(String strategy, String symbol, List<StrategySignal> signals) {}
}
