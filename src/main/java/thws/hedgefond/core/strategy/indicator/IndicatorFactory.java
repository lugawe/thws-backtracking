package thws.hedgefond.core.strategy.indicator;

import java.math.BigDecimal;

import thws.hedgefond.core.strategy.IndicatorDefinition;
import thws.hedgefond.core.strategy.StrategyException;

public final class IndicatorFactory {

    private IndicatorFactory() {}

    public static Indicator create(IndicatorDefinition definition) {
        if (definition.arguments().size() < 2
                || !"close".equals(definition.arguments().get(0))) {
            throw new StrategyException("Indicators currently require close as their first argument");
        }
        int period = Integer.parseInt(definition.arguments().get(1));
        return switch (definition.type()) {
            case "sma" -> (candles, index) -> Indicators.sma(candles, index, period);
            case "ema" -> (candles, index) -> Indicators.ema(candles, index, period);
            case "rsi" -> (candles, index) -> Indicators.rsi(candles, index, period);
            case "bollinger" -> {
                if (definition.arguments().size() != 3)
                    throw new StrategyException("bollinger requires period and deviations");
                BigDecimal deviations = new BigDecimal(definition.arguments().get(2));
                yield (candles, index) -> Indicators.bollinger(candles, index, period, deviations);
            }
            default -> throw new StrategyException("Unknown indicator: " + definition.type());
        };
    }
}
