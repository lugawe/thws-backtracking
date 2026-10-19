package thws.hedgefond.core.strategy;

import java.util.Map;

public record StrategyDefinition(
        String name,
        Map<String, IndicatorDefinition> indicators,
        Condition buy,
        Condition sell,
        Action buyAction,
        Action sellAction) {}
