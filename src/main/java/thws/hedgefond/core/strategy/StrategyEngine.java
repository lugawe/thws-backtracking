package thws.hedgefond.core.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thws.hedgefond.core.marketdata.Candle;
import thws.hedgefond.core.strategy.indicator.IndicatorFactory;

public class StrategyEngine {

    public List<StrategySignal> evaluate(StrategyDefinition strategy, List<Candle> candles) {
        Map<String, List<Object>> values = new HashMap<>();
        strategy.indicators().forEach((name, definition) -> {
            var indicator = IndicatorFactory.create(definition);
            List<Object> result = new ArrayList<>();
            for (int index = 0; index < candles.size(); index++) result.add(indicator.calculate(candles, index));
            values.put(name, result);
        });
        EvaluationContext context = new EvaluationContext(candles, values);
        List<StrategySignal> signals = new ArrayList<>();
        for (int index = 0; index < candles.size(); index++) {
            if (strategy.buy() != null && strategy.buy().matches(context, index)) {
                signals.add(new StrategySignal(
                        candles.get(index).timestamp(),
                        strategy.buyAction(),
                        candles.get(index).close()));
            }
            if (strategy.sell() != null && strategy.sell().matches(context, index)) {
                signals.add(new StrategySignal(
                        candles.get(index).timestamp(),
                        strategy.sellAction(),
                        candles.get(index).close()));
            }
        }
        return signals;
    }
}
