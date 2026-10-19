package thws.hedgefond.core.strategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.strategy.indicator.BollingerBands;

public record EvaluationContext(List<Candle> candles, Map<String, List<Object>> values) {

    public BigDecimal resolve(String reference, int index) {
        if (reference.matches("-?\\d+(\\.\\d+)?")) {
            return new BigDecimal(reference);
        }
        if ("close".equals(reference)) {
            return candles.get(index).close();
        }
        String[] parts = reference.split("\\.", 2);
        Object value = values.get(parts[0]).get(index);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BollingerBands bands && parts.length == 2) {
            return switch (parts[1]) {
                case "upper" -> bands.upper();
                case "middle" -> bands.middle();
                case "lower" -> bands.lower();
                default -> throw new StrategyException("Unknown Bollinger value: " + parts[1]);
            };
        }
        return null;
    }
}
